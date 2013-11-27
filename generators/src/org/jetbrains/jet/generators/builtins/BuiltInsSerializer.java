/*
 * Copyright 2010-2013 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.jet.generators.builtins;

import com.google.common.collect.ImmutableSet;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.io.FileUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.JetTestUtils;
import org.jetbrains.jet.cli.jvm.compiler.JetCoreEnvironment;
import org.jetbrains.jet.config.CompilerConfiguration;
import org.jetbrains.jet.descriptors.serialization.*;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.ModuleDescriptor;
import org.jetbrains.jet.lang.descriptors.PackageFragmentDescriptor;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.resolve.lazy.LazyResolveTestUtil;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.types.lang.BuiltInsSerializationUtil;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;
import org.jetbrains.jet.test.util.DescriptorValidator;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

public class BuiltInsSerializer {
    private static final String BUILT_INS_SRC_DIR = "idea/builtinsSrc";
    public static final String DEST_DIR = "compiler/frontend/builtins";

    private static int totalSize = 0;
    private static int totalFiles = 0;

    private BuiltInsSerializer() {
    }

    public static void main(String[] args) throws IOException {
        System.setProperty("java.awt.headless", "true");
        serializeToDir(new File(DEST_DIR), System.out);
    }

    public static void serializeToDir(final File destDir, @Nullable final PrintStream out) throws IOException {
        Disposable rootDisposable = Disposer.newDisposable();
        try {
            List<File> sourceFiles = FileUtil.findFilesByMask(Pattern.compile(".*\\.kt"), new File(BUILT_INS_SRC_DIR));
            CompilerConfiguration configuration = new CompilerConfiguration();
            JetCoreEnvironment environment = JetCoreEnvironment.createForTests(rootDisposable, configuration);
            List<JetFile> files = JetTestUtils.loadToJetFiles(environment, sourceFiles);

            ModuleDescriptor module = LazyResolveTestUtil.resolveLazily(files, environment, false);
            PackageFragmentDescriptor packageFragment = DescriptorUtils.getExactlyOnePackageFragment(
                    module, KotlinBuiltIns.BUILT_INS_PACKAGE_FQ_NAME);
            DescriptorValidator.validate(packageFragment);

            if (!FileUtil.delete(destDir)) {
                System.err.println("Could not delete: " + destDir);
            }
            if (!destDir.mkdirs()) {
                System.err.println("Could not make directories: " + destDir);
            }

            DescriptorSerializer serializer = new DescriptorSerializer(new SerializerExtension() {
                private final ImmutableSet<String> set = ImmutableSet.of("Any", "Nothing");

                @Override
                public boolean hasSupertypes(@NotNull ClassDescriptor classDescriptor) {
                    return !set.contains(classDescriptor.getName().asString());
                }
            });

            final List<Name> classNames = new ArrayList<Name>();
            List<DeclarationDescriptor> allDescriptors = DescriptorSerializer.sort(packageFragment.getMemberScope().getAllDescriptors());
            ClassSerializationUtil.serializeClasses(allDescriptors, serializer, new ClassSerializationUtil.Sink() {
                @Override
                public void writeClass(@NotNull ClassDescriptor classDescriptor, @NotNull ProtoBuf.Class classProto) {
                    try {
                        ByteArrayOutputStream stream = new ByteArrayOutputStream();
                        classProto.writeTo(stream);
                        write(destDir, getFileName(classDescriptor), stream, out);

                        if (DescriptorUtils.isTopLevelDeclaration(classDescriptor)) {
                            classNames.add(classDescriptor.getName());
                        }
                    }
                    catch (IOException e) {
                        throw new AssertionError(e);
                    }
                }
            });

            ByteArrayOutputStream classNamesStream = new ByteArrayOutputStream();
            writeClassNames(serializer, classNames, classNamesStream);
            write(destDir, BuiltInsSerializationUtil.getClassNamesFilePath(packageFragment), classNamesStream, out);

            ByteArrayOutputStream packageStream = new ByteArrayOutputStream();
            ProtoBuf.Package packageProto = serializer.packageProto(Collections.singleton(packageFragment)).build();
            packageProto.writeTo(packageStream);
            write(destDir, BuiltInsSerializationUtil.getPackageFilePath(packageFragment), packageStream, out);

            ByteArrayOutputStream nameStream = new ByteArrayOutputStream();
            NameSerializationUtil.serializeNameTable(nameStream, serializer.getNameTable());
            write(destDir, BuiltInsSerializationUtil.getNameTableFilePath(packageFragment), nameStream, out);

            if (out != null) {
                out.println("Total bytes written: " + totalSize + " to " + totalFiles + " files");
            }
        }
        finally {
            Disposer.dispose(rootDisposable);
        }
    }

    private static void writeClassNames(
            @NotNull DescriptorSerializer serializer,
            @NotNull List<Name> classNames,
            @NotNull ByteArrayOutputStream stream
    ) throws IOException {
        DataOutputStream data = new DataOutputStream(stream);
        try {
            data.writeInt(classNames.size());
            for (Name className : classNames) {
                int index = serializer.getNameTable().getSimpleNameIndex(className);
                data.writeInt(index);
            }
        }
        finally {
            data.close();
        }
    }

    private static void write(
            @NotNull File destDir,
            @NotNull String fileName,
            @NotNull ByteArrayOutputStream stream,
            @Nullable PrintStream out
    ) throws IOException {
        totalSize += stream.size();
        totalFiles++;
        FileUtil.writeToFile(new File(destDir, fileName), stream.toByteArray());
        if (out != null) {
            out.println(stream.size() + " bytes written to " + fileName);
        }
    }

    @NotNull
    private static String getFileName(@NotNull ClassDescriptor classDescriptor) {
        return BuiltInsSerializationUtil.getClassMetadataPath(ClassSerializationUtil.getClassId(classDescriptor));
    }
}
