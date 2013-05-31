package org.jetbrains.jet.generators.builtins;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.io.FileUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.JetTestUtils;
import org.jetbrains.jet.cli.jvm.compiler.JetCoreEnvironment;
import org.jetbrains.jet.config.CompilerConfiguration;
import org.jetbrains.jet.descriptors.serialization.*;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.lazy.LazyResolveTestUtil;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.Name;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

public class BuiltInsSerializer {
    public static final String CLASS_METADATA_FILE_EXTENSION = "kotlin_class";
    public static final String PACKAGE_FILE_NAME = ".kotlin_package";
    public static final Name CLASS_OBJECT_NAME = Name.identifier("object");

    private static final String BUILT_INS_SRC_DIR = "compiler/frontend/src";
    private static final String DEST_DIR = "compiler/frontend/builtins";
    private static final String BUILT_INS_PACKAGE_NAME = "jet";

    private static int totalSize = 0;
    private static int totalFiles = 0;

    public static void main(String[] args) throws IOException {
        List<File> sourceFiles = FileUtil.findFilesByMask(Pattern.compile(".*\\.jet"), new File(BUILT_INS_SRC_DIR));
        Disposable rootDisposable = Disposer.newDisposable();
        CompilerConfiguration configuration = new CompilerConfiguration();
        JetCoreEnvironment environment = new JetCoreEnvironment(rootDisposable, configuration);
        List<JetFile> files = JetTestUtils.loadToJetFiles(environment, sourceFiles);

        ModuleDescriptor module = LazyResolveTestUtil.resolveLazily(files, environment);

        FqName fqName = FqName.topLevel(Name.identifier(BUILT_INS_PACKAGE_NAME));
        NamespaceDescriptor namespace = module.getNamespace(fqName);
        assert namespace != null : "No built-ins namespace: " + fqName;
        Collection<DeclarationDescriptor> allDescriptors = namespace.getMemberScope().getAllDescriptors();

        final File destDir = new File(new File(DEST_DIR), BUILT_INS_PACKAGE_NAME);
        if (!FileUtil.delete(destDir)) {
            System.err.println("Could not delete: " + destDir);
        }
        if (!destDir.mkdirs()) {
            System.err.println("Could not make directories: " + destDir);
        }

        ClassSerializationUtil.serializeClasses(allDescriptors, ClassSerializationUtil.NEW_EVERY_TIME, new ClassSerializationUtil.Sink() {
            @Override
            public void writeClass(
                    @NotNull ClassDescriptor classDescriptor, @NotNull ProtoBuf.Class classProto
            ) {
                try {
                    ByteArrayOutputStream stream = new ByteArrayOutputStream();
                    classProto.writeTo(stream);

                    write(destDir, getFileName(classDescriptor), stream);
                }
                catch (IOException e) {
                    throw new AssertionError(e);
                }
            }
        });

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        DescriptorSerializer serializer = new DescriptorSerializer(NameTable.Namer.DEFAULT);
        for (DeclarationDescriptor descriptor : allDescriptors) {
            if (descriptor instanceof CallableMemberDescriptor) {
                CallableMemberDescriptor callableMemberDescriptor = (CallableMemberDescriptor) descriptor;
                ProtoBuf.Callable proto = serializer.callableProto(callableMemberDescriptor).build();
                proto.writeDelimitedTo(stream);
            }
        }

        write(destDir, getPackageFileName(namespace), stream);

        System.out.println("Total bytes written: " + totalSize + " to " + totalFiles + " files");
    }

    private static void write(File destDir, String fileName, ByteArrayOutputStream stream) throws IOException {
        totalSize += stream.size();
        totalFiles++;
        FileUtil.writeToFile(new File(destDir, fileName), stream.toByteArray());
        System.out.println(stream.size() + " bytes written to " + fileName);
    }

    private static String getPackageFileName(NamespaceDescriptor packageDescriptor) {
        return PACKAGE_FILE_NAME;
    }

    private static String getFileName(ClassDescriptor classDescriptor) {
        ClassId classId = ClassSerializationUtil.getClassId(classDescriptor, new NameTable.Namer() {
            @NotNull
            @Override
            public Name getClassName(@NotNull ClassDescriptor classDescriptor) {
                return classDescriptor.getKind() == ClassKind.CLASS_OBJECT ? CLASS_OBJECT_NAME : classDescriptor.getName();
            }

            @NotNull
            @Override
            public Name getPackageName(@NotNull NamespaceDescriptor namespaceDescriptor) {
                return namespaceDescriptor.getName();
            }
        });
        return classId.getRelativeClassName().asString() + "." + CLASS_METADATA_FILE_EXTENSION;
    }
}
