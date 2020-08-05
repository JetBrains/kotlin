package com.jetbrains.swift.codeinsight.resolve;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.*;
import com.intellij.openapi.util.io.FileFilters;
import com.intellij.openapi.util.io.FileUtilRt;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.util.CachedValue;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValueProvider.Result;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.util.containers.ContainerUtil;
import com.jetbrains.cidr.apple.gradle.GradleAppleWorkspaceListener;
import com.jetbrains.cidr.lang.CLanguageKind;
import com.jetbrains.cidr.lang.workspace.OCResolveConfiguration;
import com.jetbrains.cidr.lang.workspace.OCWorkspace;
import com.jetbrains.cidr.lang.workspace.headerRoots.AppleFramework;
import com.jetbrains.cidr.lang.workspace.headerRoots.FrameworksRoot;
import com.jetbrains.cidr.lang.workspace.headerRoots.HeadersSearchRootProcessor;
import com.jetbrains.cidr.lang.workspace.headerRoots.RealFramework;
import com.jetbrains.cidr.xcode.Xcode;
import com.jetbrains.cidr.xcode.frameworks.ApplePlatform;
import com.jetbrains.cidr.xcode.frameworks.AppleSdk;
import com.jetbrains.cidr.xcode.frameworks.AppleSdkManager;
import com.jetbrains.cidr.xcode.frameworks.buildSystem.ArchitectureValue;
import com.jetbrains.swift.SwiftCompilerSettings;
import com.jetbrains.swift.codeinsight.resolve.module.SdkInfo;
import com.jetbrains.swift.codeinsight.resolve.module.SwiftModuleReader;
import com.jetbrains.swift.sourcekitd.SourceKitServiceManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

public class MobileSwiftModuleManager extends SwiftModuleManagerBase {
    private static final Key<CachedValue<SdkInfo>> SDK_INFO_KEY = Key.create("SDK_INFO_KEY");

    public MobileSwiftModuleManager(Project project) {
        super(project);

        //noinspection Convert2Lambda
        project.getMessageBus().connect().subscribe(GradleAppleWorkspaceListener.TOPIC, new GradleAppleWorkspaceListener() {
            @Override
            public void workspaceUpdated() {
                clearSourceModuleCache();
                clearModuleCache();
            }
        });
    }

    @Override
    @Nullable
    protected SwiftModule readLibraryModule(@NotNull OCResolveConfiguration configuration, @NotNull String moduleName) {
        SwiftLibraryModule module = SwiftModuleReader.getInstance().readModule(configuration, moduleName);

        if (!module.getFiles().isEmpty()) {
            return module;
        }

        SwiftModule framework = tryObjcFramework(configuration, moduleName);
        if (framework != null) {
            return framework;
        }
        return module;
    }

    @Nullable
    private static SwiftModule tryObjcFramework(@NotNull OCResolveConfiguration configuration, @NotNull String frameworkName) {
        List<FrameworksRoot> roots = configuration.getCompilerSettings(CLanguageKind.OBJ_C).getHeadersSearchRoots().getFrameworksRoots();

        Ref<RealFramework> frameworkRef = Ref.create();
        Set<String> processed = new HashSet<>();
        for (FrameworksRoot root : roots) {
            if (!root.processFrameworks(new HeadersSearchRootProcessor() {
                @NotNull
                @Override
                public FrameworkResult processFramework(@NotNull AppleFramework framework) {
                    if (!framework.getName().equals(frameworkName) || !(framework instanceof RealFramework)) {
                        return FrameworkResult.SKIP_CHILDREN;
                    }
                    frameworkRef.set((RealFramework) framework);
                    return FrameworkResult.ABORT;
                }

                @Override
                public boolean process(@NotNull VirtualFile file) {
                    return true;
                }
            }, processed, true)) {
                break;
            }
        }

        RealFramework framework = frameworkRef.get();
        if (framework == null) return null;

        return new SwiftObjcFrameworkModule(framework, configuration);
    }

    @Override
    @Nullable
    public String getProductModuleName(@NotNull OCResolveConfiguration configuration) {
        return null;
    }

    @NotNull
    @Override
    public List<Pair<OCResolveConfiguration, List<String>>> getModulesToBuildSymbols() {
        List<Pair<OCResolveConfiguration, List<String>>> result = new ArrayList<>();
        ApplicationManager.getApplication().runReadAction(() -> {
            if (myProject.isDisposed()) return;

            Set<SdkInfo> configs = new HashSet<>();
            for (OCResolveConfiguration resolveConfiguration : OCWorkspace.getInstance(myProject).getConfigurations()) {
                AppleSdk sdk = getSdk();
                if (sdk != null) {
                    SdkInfo sdkInfo = getAppleSdkInfo(resolveConfiguration);

                    if (sdkInfo == null || !configs.add(sdkInfo)) {
                        continue;
                    }
                }

                result.add(Pair.create(resolveConfiguration, getAvailableSwiftModuleNames(resolveConfiguration)));
            }
        });
        return result;
    }

    @Override
    @NotNull
    public List<String> getAvailableSwiftModuleNames(@NotNull OCResolveConfiguration configuration) {
        AppleSdk sdk = getSdk();
        if (sdk == null) return Collections.emptyList();

        List<File> files = new ArrayList<>();
        for (File path : new File[] {getSwiftToolchainModulesPath(configuration), sdk.getSubFile("usr/lib/swift")}) {
            if (path != null) {
                File[] swiftModules = path.listFiles(FileFilters.withExtension("swiftmodule"));
                if (swiftModules != null) {
                    files.addAll(Arrays.asList(swiftModules));
                }
            }
        }
        if (files.isEmpty()) return Collections.emptyList();

        Set<String> initialModules =
                files.stream().map(file1 -> FileUtilRt.getNameWithoutExtension(file1.getName())).collect(Collectors.toSet());

        sdk.processFrameworkFiles(file -> {
            initialModules.add(file.getNameWithoutExtension());
            return true;
        });
        return new ArrayList<>(initialModules);
    }

    @Override
    @Nullable
    public File getSwiftToolchainModulesPath(@NotNull OCResolveConfiguration configuration) {
        AppleSdk sdk = getSdk();
        if (sdk == null) {
            return null;
        }
        File toolchainLibPath = SourceKitServiceManager.Companion.getInstance().getToolchainLibPath();
        File modulesPath = new File(toolchainLibPath, "swift/" + sdk.getPlatform().getType().getPlatformName());

        if (Xcode.getVersion().lessThan(11)) {
            List<ArchitectureValue> architectures = getProductArchitectures();
            ContainerUtil.sort(architectures, (arch1, arch2) -> {
                if (arch1.getBits() < arch2.getBits()) return -1;
                if (arch1.getBits() > arch2.getBits()) return +1;
                return StringUtil.compare(arch1.getId(), arch2.getId(), false);
            });
            ArchitectureValue last = ContainerUtil.getLastItem(architectures, null);
            if (last != null) {
                modulesPath = new File(modulesPath, last.getId());
            }
        }
        else {
            modulesPath = new File(modulesPath, "prebuilt-modules");
        }

        return modulesPath;
    }

    @Nullable
    @Override
    public File getSwiftToolchainApinotesPath(@NotNull OCResolveConfiguration configuration) {
        File modulesPath = getSwiftToolchainModulesPath(configuration);
        if (modulesPath != null && Xcode.getVersion().isOrGreaterThan(11)) {
            modulesPath = new File(modulesPath.getParentFile(), "apinotes");
        }
        return modulesPath;
    }

    @Override
    @Nullable
    public SdkInfo getSdkInfo(@NotNull OCResolveConfiguration configuration) {
        return getAppleSdkInfo(configuration);
    }

    @Nullable
    public static SdkInfo getAppleSdkInfo(@NotNull OCResolveConfiguration configuration) {
        CachedValueProvider<SdkInfo> provider = () -> {
            SdkInfo sdkInfo = doInferSdkInfo(configuration);
            ModificationTracker tracker = OCWorkspace.getInstance(configuration.getProject())
                    .getModificationTrackers().getCompilerSettingsTracker();
            return Result.create(sdkInfo, tracker);
        };

        return CachedValuesManager.getManager(configuration.getProject()).getCachedValue(configuration, SDK_INFO_KEY, provider, false);
    }

    @Nullable
    private static SdkInfo doInferSdkInfo(@NotNull OCResolveConfiguration configuration) {
        List<ArchitectureValue> architectures = getProductArchitectures(); // .stream().sorted().collect(Collectors.toList());
        AppleSdk sdk = getSdk();

        if (architectures.isEmpty() || sdk == null) {
            return null;
        }

        ArchitectureValue architecture = ContainerUtil.getLastItem(architectures);
        assert architecture != null;
        String platform = platformName(sdk.getPlatform());

        Version version = SwiftCompilerSettings.getSwiftVersion(configuration);
        return new SdkInfo(
                sdk.getName(), platform, architecture.toString(), sdk.getHomePath(), version, sdk.getVariant(), sdk.getVersionString());
    }

    @Nullable
    private static AppleSdk getSdk() {
        return ContainerUtil.getFirstItem(AppleSdkManager.getInstance().findSdksForPlatform(ApplePlatform.Type.IOS_SIMULATOR));
    }

    @NotNull
    private static List<ArchitectureValue> getProductArchitectures() {
        return Collections.singletonList(ArchitectureValue.x86_64);
    }

    @NotNull
    public static String platformName(@NotNull ApplePlatform platform) {
        if (platform.isIOS()) return "ios";
        if (platform.isTv()) return "tvos";
        return platform.getName();
    }

}
