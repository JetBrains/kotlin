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
import org.jetbrains.jet.lang.types.lang.BuiltInsSerializationUtil;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

public class BuiltInsSerializer {
    private static final String BUILT_INS_SRC_DIR = "compiler/frontend/src";
    private static final String DEST_DIR = "compiler/frontend/" + BuiltInsSerializationUtil.BUILT_INS_DIR;

    private static int totalSize = 0;
    private static int totalFiles = 0;

    public static void main(String[] args) throws IOException {
        List<File> sourceFiles = FileUtil.findFilesByMask(Pattern.compile(".*\\.jet"), new File(BUILT_INS_SRC_DIR));
        Disposable rootDisposable = Disposer.newDisposable();
        CompilerConfiguration configuration = new CompilerConfiguration();
        JetCoreEnvironment environment = new JetCoreEnvironment(rootDisposable, configuration);
        List<JetFile> files = JetTestUtils.loadToJetFiles(environment, sourceFiles);

        ModuleDescriptor module = LazyResolveTestUtil.resolveLazily(files, environment);

        FqName fqName = FqName.topLevel(Name.identifier(KotlinBuiltIns.BUILT_INS_PACKAGE_NAME_STRING));
        NamespaceDescriptor namespace = module.getNamespace(fqName);
        assert namespace != null : "No built-ins namespace: " + fqName;
        Collection<DeclarationDescriptor> allDescriptors = namespace.getMemberScope().getAllDescriptors();

        final File destDir = new File(DEST_DIR);
        if (!FileUtil.delete(destDir)) {
            System.err.println("Could not delete: " + destDir);
        }
        if (!destDir.mkdirs()) {
            System.err.println("Could not make directories: " + destDir);
        }

        DescriptorSerializer serializer = new DescriptorSerializer(NameTable.Namer.DEFAULT);
        ClassSerializationUtil.serializeClasses(allDescriptors, ClassSerializationUtil.constantSerializer(serializer), new ClassSerializationUtil.Sink() {
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
        for (DeclarationDescriptor descriptor : allDescriptors) {
            if (descriptor instanceof CallableMemberDescriptor) {
                CallableMemberDescriptor callableMemberDescriptor = (CallableMemberDescriptor) descriptor;
                ProtoBuf.Callable proto = serializer.callableProto(callableMemberDescriptor).build();
                proto.writeDelimitedTo(stream);
            }
        }

        write(destDir, BuiltInsSerializationUtil.getPackageFilePath(namespace), stream);

        ByteArrayOutputStream nameStream = new ByteArrayOutputStream();
        NameSerializationUtil.serializeNameTable(nameStream, serializer.getNameTable());
        write(destDir, BuiltInsSerializationUtil.getNameTableFilePath(namespace), nameStream);

        System.out.println("Total bytes written: " + totalSize + " to " + totalFiles + " files");
    }

    private static void write(File destDir, String fileName, ByteArrayOutputStream stream) throws IOException {
        totalSize += stream.size();
        totalFiles++;
        FileUtil.writeToFile(new File(destDir, fileName), stream.toByteArray());
        System.out.println(stream.size() + " bytes written to " + fileName);
    }

    private static String getFileName(ClassDescriptor classDescriptor) {
        return BuiltInsSerializationUtil.getClassMetadataPath(BuiltInsSerializationUtil.getClassId(classDescriptor));
    }
}
