/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.versions;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileTypes.StdFileTypes;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.indexing.*;
import com.intellij.util.io.ExternalIntegerKeyDescriptor;
import com.intellij.util.io.KeyDescriptor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.load.java.AbiVersionUtil;
import org.jetbrains.org.objectweb.asm.AnnotationVisitor;
import org.jetbrains.org.objectweb.asm.ClassReader;
import org.jetbrains.org.objectweb.asm.ClassVisitor;
import org.jetbrains.org.objectweb.asm.Opcodes;

import java.util.Map;
import java.util.Set;

import static org.jetbrains.kotlin.codegen.AsmUtil.asmDescByFqNameWithoutInnerClasses;
import static org.jetbrains.kotlin.load.java.JvmAnnotationNames.*;

/**
 * Important! This is not a stub-based index. And it has its own version
 */
public class KotlinAbiVersionIndex extends ScalarIndexExtension<Integer> constructor {
    private static final Logger LOG = Logger.getInstance(KotlinAbiVersionIndex.class);

    public static final KotlinAbiVersionIndex INSTANCE = new KotlinAbiVersionIndex();

    private static final int VERSION = 1;

    private static final ID<Integer, Void> NAME = ID.create(KotlinAbiVersionIndex.class.getCanonicalName());
    private static final ExternalIntegerKeyDescriptor KEY_DESCRIPTOR = new ExternalIntegerKeyDescriptor();

    private static final FileBasedIndex.InputFilter INPUT_FILTER = new FileBasedIndex.InputFilter() {
        @Override
        public boolean acceptInput(VirtualFile file) {
            return file.getFileType() == StdFileTypes.CLASS;
        }
    };

    private static final DataIndexer<Integer, Void, FileContent> INDEXER = new DataIndexer<Integer, Void, FileContent>() {
        @SuppressWarnings("deprecation")
        private final Set<String> kotlinAnnotationsDesc = new ImmutableSet.Builder<String>()
                .add(asmDescByFqNameWithoutInnerClasses(OLD_JET_CLASS_ANNOTATION))
                .add(asmDescByFqNameWithoutInnerClasses(OLD_JET_PACKAGE_CLASS_ANNOTATION))
                .add(asmDescByFqNameWithoutInnerClasses(OLD_KOTLIN_CLASS))
                .add(asmDescByFqNameWithoutInnerClasses(OLD_KOTLIN_PACKAGE))
                .add(asmDescByFqNameWithoutInnerClasses(KOTLIN_CLASS))
                .add(asmDescByFqNameWithoutInnerClasses(KOTLIN_PACKAGE))
                .build();

        @NotNull
        @Override
        public Map<Integer, Void> map(FileContent inputData) {
            final Map<Integer, Void> result = Maps.newHashMap();
            final Ref<Boolean> annotationPresent = new Ref<Boolean>(false);

            try {
                ClassReader classReader = new ClassReader(inputData.getContent());
                classReader.accept(new ClassVisitor(Opcodes.ASM5) {
                    @Override
                    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
                        if (!kotlinAnnotationsDesc.contains(desc)) {
                            return null;
                        }
                        annotationPresent.set(true);
                        return new AnnotationVisitor(Opcodes.ASM5) {
                            @Override
                            public void visit(String name, Object value) {
                                if (ABI_VERSION_FIELD_NAME.equals(name)) {
                                    if (value instanceof Integer) {
                                        Integer abiVersion = (Integer) value;
                                        result.put(abiVersion, null);
                                    }
                                    else {
                                        // Version is set to something weird
                                        result.put(AbiVersionUtil.INVALID_VERSION, null);
                                    }
                                }
                            }
                        };
                    }
                }, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
            }
            catch (Throwable e) {
                LOG.warn("Could not index ABI version for file " + inputData.getFile() + ": " + e.getMessage());
            }

            if (annotationPresent.get() && result.isEmpty()) {
                // No version at all: the class is too old
                result.put(AbiVersionUtil.INVALID_VERSION, null);
            }

            return result;
        }
    };

    private KotlinAbiVersionIndex() {
    }

    @NotNull
    @Override
    public ID<Integer, Void> getName() {
        return NAME;
    }

    @NotNull
    @Override
    public DataIndexer<Integer, Void, FileContent> getIndexer() {
        return INDEXER;
    }

    @Override
    public KeyDescriptor<Integer> getKeyDescriptor() {
        return KEY_DESCRIPTOR;
    }

    @Override
    public FileBasedIndex.InputFilter getInputFilter() {
        return INPUT_FILTER;
    }

    @Override
    public boolean dependsOnFileContent() {
        return true;
    }

    @Override
    public int getVersion() {
        return VERSION;
    }
}
