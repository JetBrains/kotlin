package org.jetbrains.jet.generators.builtins;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableSet;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.io.FileUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.JetTestUtils;
import org.jetbrains.jet.cli.jvm.compiler.JetCoreEnvironment;
import org.jetbrains.jet.config.CompilerConfiguration;
import org.jetbrains.jet.descriptors.serialization.ClassSerializationUtil;
import org.jetbrains.jet.descriptors.serialization.DescriptorSerializer;
import org.jetbrains.jet.descriptors.serialization.NameSerializationUtil;
import org.jetbrains.jet.descriptors.serialization.ProtoBuf;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.resolve.lazy.LazyResolveTestUtil;
import org.jetbrains.jet.lang.types.lang.BuiltInsSerializationUtil;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;
import org.jetbrains.jet.test.util.DescriptorValidator;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.regex.Pattern;

public class BuiltInsSerializer {
    private static final String BUILT_INS_SRC_DIR = "idea/builtinsSrc";
    private static final String DEST_DIR = "compiler/frontend/builtins";

    private static int totalSize = 0;
    private static int totalFiles = 0;

    private BuiltInsSerializer() {
    }

    public static void main(String[] args) throws IOException {
        List<File> sourceFiles = FileUtil.findFilesByMask(Pattern.compile(".*\\.jet"), new File(BUILT_INS_SRC_DIR));
        Disposable rootDisposable = Disposer.newDisposable();
        CompilerConfiguration configuration = new CompilerConfiguration();
        JetCoreEnvironment environment = new JetCoreEnvironment(rootDisposable, configuration);
        List<JetFile> files = JetTestUtils.loadToJetFiles(environment, sourceFiles);

        ModuleDescriptor module = LazyResolveTestUtil.resolveLazily(files, environment, false);

        NamespaceDescriptor namespace = module.getNamespace(KotlinBuiltIns.BUILT_INS_PACKAGE_FQ_NAME);
        assert namespace != null : "No built-ins namespace: " + KotlinBuiltIns.BUILT_INS_PACKAGE_FQ_NAME;

        DescriptorValidator.validate(namespace);

        List<DeclarationDescriptor> allDescriptors = DescriptorSerializer.sort(namespace.getMemberScope().getAllDescriptors());

        final File destDir = new File(DEST_DIR);
        if (!FileUtil.delete(destDir)) {
            System.err.println("Could not delete: " + destDir);
        }
        if (!destDir.mkdirs()) {
            System.err.println("Could not make directories: " + destDir);
        }

        final DescriptorSerializer serializer = new DescriptorSerializer(new Predicate<ClassDescriptor>() {
            private final ImmutableSet<String> set = ImmutableSet.of("Any", "Nothing");

            @Override
            public boolean apply(ClassDescriptor classDescriptor) {
                return set.contains(classDescriptor.getName().asString());
            }
        });
        ByteArrayOutputStream classNames = new ByteArrayOutputStream();
        final DataOutputStream data = new DataOutputStream(classNames);
        ClassSerializationUtil.serializeClasses(allDescriptors, ClassSerializationUtil.constantSerializer(serializer), new ClassSerializationUtil.Sink() {
            @Override
            public void writeClass(
                    @NotNull ClassDescriptor classDescriptor, @NotNull ProtoBuf.Class classProto
            ) {
                try {
                    ByteArrayOutputStream stream = new ByteArrayOutputStream();
                    classProto.writeTo(stream);

                    write(destDir, getFileName(classDescriptor), stream);

                    if (DescriptorUtils.isTopLevelDeclaration(classDescriptor)) {
                        int index = serializer.getNameTable().getSimpleNameIndex(classDescriptor.getName());
                        data.writeInt(index);
                    }
                }
                catch (IOException e) {
                    throw new AssertionError(e);
                }
            }
        });
        data.close();

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

        write(destDir, BuiltInsSerializationUtil.getClassNamesFilePath(namespace), classNames);

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
