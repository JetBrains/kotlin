package com.jetbrains.swift.codeinsight.resolve;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.ModificationTracker;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.util.containers.ContainerUtil;
import com.jetbrains.cidr.lang.CLanguageKind;
import com.jetbrains.cidr.lang.preprocessor.OCInclusionContext;
import com.jetbrains.cidr.lang.psi.OCFile;
import com.jetbrains.cidr.lang.workspace.OCResolveConfiguration;
import com.jetbrains.cidr.xcode.Xcode;
import com.jetbrains.swift.codeinsight.resolve.processor.SwiftModuleDependenciesImportUtils;
import com.jetbrains.swift.symbols.SwiftObjcSymbolsConverter;
import com.jetbrains.swift.symbols.swiftoc.SwiftObjcSymbolsConverterImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;

class MobileSwiftBridgingUtil {
    @NotNull
    static SwiftGlobalSymbols buildBridgedSymbols(
            @NotNull List<VirtualFile> headers,
            @NotNull OCResolveConfiguration configuration,
            @NotNull String moduleName,
            @NotNull Project project
    ) {
        SwiftMultiGlobalSymbols result = new SwiftMultiGlobalSymbols();
        SwiftGlobalSymbolsImpl bridgedSymbols = new SwiftGlobalSymbolsImpl(SwiftGlobalSymbols.SymbolsOrigin.OBJC);

        SwiftGlobalSymbolsImpl.SymbolProcessor processor = new SwiftGlobalSymbolsImpl.SymbolProcessor(bridgedSymbols, false);
        PsiManager psiManager = PsiManager.getInstance(project);

        for (VirtualFile header : headers) {
            if (!header.isValid()) continue;
            PsiFile file = psiManager.findFile(header);
            if (!(file instanceof OCFile)) continue;

            OCInclusionContext context = OCInclusionContext.beforePCHFileContext(configuration, CLanguageKind.OBJ_C, file);
            SwiftObjcSymbolsConverter converter =
                    new SwiftObjcSymbolsConverterImpl(project, context, file, configuration,
                                                      virtualFile -> !shouldSkipBuildingBridgedSymbols(virtualFile, project));
            converter.processSymbols(processor);
        }

        if (!processor.isAnyProcessed()) return SwiftGlobalSymbols.EMPTY;
        result.addProvider(bridgedSymbols);

        SwiftResolveService resolveService = SwiftResolveService.getInstance(project);
        Set<String> visited = ContainerUtil.newHashSet(moduleName);
        bridgedSymbols.processAllImports(importName -> SwiftModuleDependenciesImportUtils
                .processModuleAndDependencies(importName, configuration, curModuleName -> {
                    if (visited.add(curModuleName)) {
                        result.addProvider(resolveService.getGlobalSymbolsForModule(configuration, curModuleName));
                    }
                    return true;
                }), new SwiftGlobalSymbols.ProcessingContext()
        );

        return result;
    }

    private static boolean shouldSkipBuildingBridgedSymbols(@Nullable VirtualFile file, @NotNull Project project) {
        if (file == null) return false;

        VirtualFile xcodeRoot = CachedValuesManager.getManager(project).getCachedValue(project, () -> CachedValueProvider.Result.create(
                LocalFileSystem.getInstance().findFileByPath(Xcode.getBasePath()),
                ModificationTracker.NEVER_CHANGED
        ));

        if (xcodeRoot == null) return false;
        return VfsUtilCore.isAncestor(xcodeRoot, file, true);
    }
}
