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

package org.jetbrains.kotlin.load.kotlin.header;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.descriptors.SourceElement;
import org.jetbrains.kotlin.load.java.JvmAnnotationNames;
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmBytecodeBinaryVersion;
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmMetadataVersion;
import org.jetbrains.kotlin.name.ClassId;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.resolve.constants.ClassLiteralValue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.jetbrains.kotlin.load.java.JvmAnnotationNames.*;
import static org.jetbrains.kotlin.load.kotlin.KotlinJvmBinaryClass.*;
import static org.jetbrains.kotlin.load.kotlin.header.KotlinClassHeader.Kind.*;

public class ReadKotlinClassHeaderAnnotationVisitor implements AnnotationVisitor {
    private static final boolean IGNORE_OLD_METADATA = "true".equals(System.getProperty("kotlin.ignore.old.metadata"));

    private static final Map<ClassId, KotlinClassHeader.Kind> HEADER_KINDS = new HashMap<ClassId, KotlinClassHeader.Kind>();

    static {
        // TODO: delete this at some point
        HEADER_KINDS.put(ClassId.topLevel(new FqName("kotlin.jvm.internal.KotlinClass")), CLASS);
        HEADER_KINDS.put(ClassId.topLevel(new FqName("kotlin.jvm.internal.KotlinFileFacade")), FILE_FACADE);
        HEADER_KINDS.put(ClassId.topLevel(new FqName("kotlin.jvm.internal.KotlinMultifileClass")), MULTIFILE_CLASS);
        HEADER_KINDS.put(ClassId.topLevel(new FqName("kotlin.jvm.internal.KotlinMultifileClassPart")), MULTIFILE_CLASS_PART);
        HEADER_KINDS.put(ClassId.topLevel(new FqName("kotlin.jvm.internal.KotlinSyntheticClass")), SYNTHETIC_CLASS);
    }

    private int[] metadataVersionArray = null;
    private JvmBytecodeBinaryVersion bytecodeVersion = null;
    private String extraString = null;
    private int extraInt = 0;
    private String packageName = null;
    private String[] data = null;
    private String[] strings = null;
    private String[] incompatibleData = null;
    private KotlinClassHeader.Kind headerKind = null;

    @Nullable
    public KotlinClassHeader createHeader() {
        if (headerKind == null || metadataVersionArray == null) {
            return null;
        }

        JvmMetadataVersion metadataVersion =
                new JvmMetadataVersion(metadataVersionArray, (extraInt & JvmAnnotationNames.METADATA_STRICT_VERSION_SEMANTICS_FLAG) != 0);

        if (!metadataVersion.isCompatible()) {
            incompatibleData = data;
            data = null;
        }
        else if (shouldHaveData() && data == null) {
            // This means that the annotation is found and its ABI version is compatible, but there's no "data" string array in it.
            // We tell the outside world that there's really no annotation at all
            return null;
        }

        return new KotlinClassHeader(
                headerKind,
                metadataVersion,
                bytecodeVersion != null ? bytecodeVersion : JvmBytecodeBinaryVersion.INVALID_VERSION,
                data,
                incompatibleData,
                strings,
                extraString,
                extraInt,
                packageName
        );
    }

    private boolean shouldHaveData() {
        return headerKind == CLASS ||
               headerKind == FILE_FACADE ||
               headerKind == MULTIFILE_CLASS_PART;
    }

    @Nullable
    @Override
    public AnnotationArgumentVisitor visitAnnotation(@NotNull ClassId classId, @NotNull SourceElement source) {
        FqName fqName = classId.asSingleFqName();
        if (fqName.equals(METADATA_FQ_NAME)) {
            return new KotlinMetadataArgumentVisitor();
        }

        if (IGNORE_OLD_METADATA) return null;

        if (headerKind != null) {
            // Ignore all Kotlin annotations except the first found
            return null;
        }

        KotlinClassHeader.Kind newKind = HEADER_KINDS.get(classId);
        if (newKind != null) {
            headerKind = newKind;
            return new OldDeprecatedAnnotationArgumentVisitor();
        }

        return null;
    }

    @Override
    public void visitEnd() {
    }

    private class KotlinMetadataArgumentVisitor implements AnnotationArgumentVisitor {
        @Override
        public void visit(@Nullable Name name, @Nullable Object value) {
            if (name == null) return;

            String string = name.asString();
            if (KIND_FIELD_NAME.equals(string)) {
                if (value instanceof Integer) {
                    headerKind = KotlinClassHeader.Kind.getById((Integer) value);
                }
            }
            else if (METADATA_VERSION_FIELD_NAME.equals(string)) {
                if (value instanceof int[]) {
                    metadataVersionArray = (int[]) value;
                }
            }
            else if (BYTECODE_VERSION_FIELD_NAME.equals(string)) {
                if (value instanceof int[]) {
                    bytecodeVersion = new JvmBytecodeBinaryVersion((int[]) value);
                }
            }
            else if (METADATA_EXTRA_STRING_FIELD_NAME.equals(string)) {
                if (value instanceof String) {
                    extraString = (String) value;
                }
            }
            else if (METADATA_EXTRA_INT_FIELD_NAME.equals(string)) {
                if (value instanceof Integer) {
                    extraInt = (Integer) value;
                }
            }
            else if (METADATA_PACKAGE_NAME_FIELD_NAME.equals(string)) {
                if (value instanceof String) {
                    packageName = (String) value;
                }
            }
        }

        @Override
        public void visitClassLiteral(@NotNull Name name, @NotNull ClassLiteralValue classLiteralValue) {
        }

        @Override
        @Nullable
        public AnnotationArrayArgumentVisitor visitArray(@NotNull Name name) {
            String string = name.asString();
            if (METADATA_DATA_FIELD_NAME.equals(string)) {
                return dataArrayVisitor();
            }
            else if (METADATA_STRINGS_FIELD_NAME.equals(string)) {
                return stringsArrayVisitor();
            }
            else {
                return null;
            }
        }

        @NotNull
        private AnnotationArrayArgumentVisitor dataArrayVisitor() {
            return new CollectStringArrayAnnotationVisitor() {
                @Override
                protected void visitEnd(@NotNull String[] result) {
                    data = result;
                }
            };
        }

        @NotNull
        private AnnotationArrayArgumentVisitor stringsArrayVisitor() {
            return new CollectStringArrayAnnotationVisitor() {
                @Override
                protected void visitEnd(@NotNull String[] result) {
                    strings = result;
                }
            };
        }

        @Override
        public void visitEnum(@NotNull Name name, @NotNull ClassId enumClassId, @NotNull Name enumEntryName) {
        }

        @Nullable
        @Override
        public AnnotationArgumentVisitor visitAnnotation(@NotNull Name name, @NotNull ClassId classId) {
            return null;
        }

        @Override
        public void visitEnd() {
        }
    }

    private class OldDeprecatedAnnotationArgumentVisitor implements AnnotationArgumentVisitor {
        @Override
        public void visit(@Nullable Name name, @Nullable Object value) {
            if (name == null) return;

            String string = name.asString();
            if ("version".equals(string)) {
                if (value instanceof int[]) {
                    metadataVersionArray = (int[]) value;

                    // If there's no bytecode binary version in the class file, we assume it to be equal to the metadata version
                    if (bytecodeVersion == null) {
                        bytecodeVersion = new JvmBytecodeBinaryVersion((int[]) value);
                    }
                }
            }
            else if ("multifileClassName".equals(string)) {
                extraString = value instanceof String ? (String) value : null;
            }
        }

        @Override
        public void visitClassLiteral(@NotNull Name name, @NotNull ClassLiteralValue classLiteralValue) {
        }

        @Override
        @Nullable
        public AnnotationArrayArgumentVisitor visitArray(@NotNull Name name) {
            String string = name.asString();
            if ("data".equals(string) || "filePartClassNames".equals(string)) {
                return dataArrayVisitor();
            }
            else if ("strings".equals(string)) {
                return stringsArrayVisitor();
            }
            else {
                return null;
            }
        }

        @NotNull
        private AnnotationArrayArgumentVisitor dataArrayVisitor() {
            return new CollectStringArrayAnnotationVisitor() {
                @Override
                protected void visitEnd(@NotNull String[] data) {
                    ReadKotlinClassHeaderAnnotationVisitor.this.data = data;
                }
            };
        }

        @NotNull
        private AnnotationArrayArgumentVisitor stringsArrayVisitor() {
            return new CollectStringArrayAnnotationVisitor() {
                @Override
                protected void visitEnd(@NotNull String[] data) {
                    strings = data;
                }
            };
        }

        @Override
        public void visitEnum(@NotNull Name name, @NotNull ClassId enumClassId, @NotNull Name enumEntryName) {
        }

        @Nullable
        @Override
        public AnnotationArgumentVisitor visitAnnotation(@NotNull Name name, @NotNull ClassId classId) {
            return null;
        }

        @Override
        public void visitEnd() {
        }
    }

    private abstract static class CollectStringArrayAnnotationVisitor implements AnnotationArrayArgumentVisitor {
        private final List<String> strings;

        public CollectStringArrayAnnotationVisitor() {
            this.strings = new ArrayList<String>();
        }

        @Override
        public void visit(@Nullable Object value) {
            if (value instanceof String) {
                strings.add((String) value);
            }
        }

        @Override
        public void visitEnum(@NotNull ClassId enumClassId, @NotNull Name enumEntryName) {
        }

        @Override
        public void visitClassLiteral(@NotNull ClassLiteralValue classLiteralValue) {
        }

        @Override
        public void visitEnd() {
            //noinspection SSBasedInspection
            visitEnd(strings.toArray(new String[strings.size()]));
        }

        protected abstract void visitEnd(@NotNull String[] data);
    }
}
