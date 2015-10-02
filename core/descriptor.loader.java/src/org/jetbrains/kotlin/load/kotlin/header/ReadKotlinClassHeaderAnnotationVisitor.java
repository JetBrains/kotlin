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
import org.jetbrains.kotlin.load.java.AbiVersionUtil;
import org.jetbrains.kotlin.name.ClassId;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.resolve.jvm.JvmClassName;
import org.jetbrains.kotlin.serialization.deserialization.BinaryVersion;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.jetbrains.kotlin.load.java.JvmAnnotationNames.*;
import static org.jetbrains.kotlin.load.kotlin.KotlinJvmBinaryClass.*;
import static org.jetbrains.kotlin.load.kotlin.header.KotlinClassHeader.Kind.*;

public class ReadKotlinClassHeaderAnnotationVisitor implements AnnotationVisitor {
    private static final Map<JvmClassName, KotlinClassHeader.Kind> HEADER_KINDS = new HashMap<JvmClassName, KotlinClassHeader.Kind>();
    private static final Map<JvmClassName, KotlinClassHeader.Kind> OLD_DEPRECATED_ANNOTATIONS_KINDS = new HashMap<JvmClassName, KotlinClassHeader.Kind>();

    static {
        HEADER_KINDS.put(KotlinClass.CLASS_NAME, CLASS);
        HEADER_KINDS.put(JvmClassName.byFqNameWithoutInnerClasses(KOTLIN_PACKAGE), PACKAGE_FACADE);
        HEADER_KINDS.put(JvmClassName.byFqNameWithoutInnerClasses(KOTLIN_FILE_FACADE), FILE_FACADE);
        HEADER_KINDS.put(JvmClassName.byFqNameWithoutInnerClasses(KOTLIN_MULTIFILE_CLASS), MULTIFILE_CLASS);
        HEADER_KINDS.put(JvmClassName.byFqNameWithoutInnerClasses(KOTLIN_MULTIFILE_CLASS_PART), MULTIFILE_CLASS_PART);
        HEADER_KINDS.put(KotlinSyntheticClass.CLASS_NAME, SYNTHETIC_CLASS);

        initOldAnnotations();
    }

    @SuppressWarnings("deprecation")
    private static void initOldAnnotations() {
        OLD_DEPRECATED_ANNOTATIONS_KINDS.put(JvmClassName.byFqNameWithoutInnerClasses(OLD_JET_CLASS_ANNOTATION), CLASS);
        OLD_DEPRECATED_ANNOTATIONS_KINDS.put(JvmClassName.byFqNameWithoutInnerClasses(OLD_JET_PACKAGE_CLASS_ANNOTATION),
                                             KotlinClassHeader.Kind.PACKAGE_FACADE);
        OLD_DEPRECATED_ANNOTATIONS_KINDS.put(JvmClassName.byFqNameWithoutInnerClasses(OLD_KOTLIN_CLASS), CLASS);
        OLD_DEPRECATED_ANNOTATIONS_KINDS.put(JvmClassName.byFqNameWithoutInnerClasses(OLD_KOTLIN_PACKAGE), PACKAGE_FACADE);
        OLD_DEPRECATED_ANNOTATIONS_KINDS.put(JvmClassName.byFqNameWithoutInnerClasses(OLD_KOTLIN_PACKAGE_FRAGMENT), SYNTHETIC_CLASS);
        OLD_DEPRECATED_ANNOTATIONS_KINDS.put(JvmClassName.byFqNameWithoutInnerClasses(OLD_KOTLIN_TRAIT_IMPL), SYNTHETIC_CLASS);
    }

    private BinaryVersion version = AbiVersionUtil.INVALID_VERSION;
    private String multifileClassName = null;
    private String[] filePartClassNames = null;
    private String[] annotationData = null;
    private String[] strings = null;
    private KotlinClassHeader.Kind headerKind = null;
    private KotlinClass.Kind classKind = null;
    private KotlinSyntheticClass.Kind syntheticClassKind = null;

    @Nullable
    public KotlinClassHeader createHeader() {
        if (headerKind == null) {
            return null;
        }

        if (headerKind == CLASS && classKind == null) {
            // Default class kind is Kind.CLASS
            classKind = KotlinClass.Kind.CLASS;
        }

        if (!AbiVersionUtil.isAbiVersionCompatible(version)) {
            return new KotlinClassHeader(headerKind, version, null, strings, classKind, syntheticClassKind, null, null);
        }

        if (shouldHaveData() && annotationData == null) {
            // This means that the annotation is found and its ABI version is compatible, but there's no "data" string array in it.
            // We tell the outside world that there's really no annotation at all
            return null;
        }

        return new KotlinClassHeader(
                headerKind, version, annotationData, strings, classKind, syntheticClassKind, filePartClassNames, multifileClassName
        );
    }

    private boolean shouldHaveData() {
        return headerKind == CLASS ||
               headerKind == PACKAGE_FACADE ||
               headerKind == FILE_FACADE ||
               headerKind == MULTIFILE_CLASS_PART;
    }

    @Nullable
    @Override
    public AnnotationArgumentVisitor visitAnnotation(@NotNull ClassId classId, @NotNull SourceElement source) {
        if (headerKind != null) {
            // Ignore all Kotlin annotations except the first found
            return null;
        }

        JvmClassName annotation = JvmClassName.byClassId(classId);

        KotlinClassHeader.Kind newKind = HEADER_KINDS.get(annotation);
        if (newKind != null) {
            headerKind = newKind;

            switch (newKind) {
                case CLASS:
                    return new ClassHeaderReader();
                case PACKAGE_FACADE:
                    return new PackageHeaderReader();
                case FILE_FACADE:
                    return new FileFacadeHeaderReader();
                case MULTIFILE_CLASS:
                    return new MultifileClassHeaderReader();
                case MULTIFILE_CLASS_PART:
                    return new MultifileClassPartHeaderReader();
                case SYNTHETIC_CLASS:
                    return new SyntheticClassHeaderReader();
                default:
                    return null;
            }
        }

        KotlinClassHeader.Kind oldAnnotationKind = OLD_DEPRECATED_ANNOTATIONS_KINDS.get(annotation);
        if (oldAnnotationKind != null) {
            headerKind = oldAnnotationKind;
        }

        return null;
    }

    @Override
    public void visitEnd() {
    }

    private abstract class HeaderAnnotationArgumentVisitor implements AnnotationArgumentVisitor {
        protected final JvmClassName annotationClassName;

        public HeaderAnnotationArgumentVisitor(@NotNull JvmClassName annotationClassName) {
            this.annotationClassName = annotationClassName;
        }

        @Override
        public void visit(@Nullable Name name, @Nullable Object value) {
            if (name == null) return;

            String string = name.asString();
            if (VERSION_FIELD_NAME.equals(string)) {
                version = value instanceof int[] ? BinaryVersion.create((int[]) value) : AbiVersionUtil.INVALID_VERSION;
            }
            else if (MULTIFILE_CLASS_NAME_FIELD_NAME.equals(string)) {
                multifileClassName = value instanceof String ? (String) value : null;
            }
            else if (OLD_ABI_VERSION_FIELD_NAME.equals(string)) {
                if (version == AbiVersionUtil.INVALID_VERSION && value instanceof Integer && (Integer) value > 0) {
                    version = BinaryVersion.create(0, (Integer) value, 0);
                }
            }
        }

        @Override
        @Nullable
        public AnnotationArrayArgumentVisitor visitArray(@NotNull Name name) {
            String string = name.asString();
            if (DATA_FIELD_NAME.equals(string)) {
                return dataArrayVisitor();
            }
            else if (STRINGS_FIELD_NAME.equals(string)) {
                return stringsArrayVisitor();
            }
            else if (FILE_PART_CLASS_NAMES_FIELD_NAME.equals(string)) {
                return filePartClassNamesVisitor();
            }
            else {
                return null;
            }
        }

        @NotNull
        private AnnotationArrayArgumentVisitor filePartClassNamesVisitor() {
            return new CollectStringArrayAnnotationVisitor() {
                @Override
                protected void visitEnd(@NotNull String[] data) {
                    filePartClassNames = data;
                }
            };
        }

        @NotNull
        private AnnotationArrayArgumentVisitor dataArrayVisitor() {
            return new CollectStringArrayAnnotationVisitor() {
                @Override
                protected void visitEnd(@NotNull String[] data) {
                    annotationData = data;
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

        private abstract class CollectStringArrayAnnotationVisitor implements AnnotationArrayArgumentVisitor {
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
            public void visitEnd() {
                //noinspection SSBasedInspection
                visitEnd(strings.toArray(new String[strings.size()]));
            }

            protected abstract void visitEnd(@NotNull String[] data);
        }
    }

    private class ClassHeaderReader extends HeaderAnnotationArgumentVisitor {
        public ClassHeaderReader() {
            super(KotlinClass.CLASS_NAME);
        }

        @Override
        public void visitEnum(@NotNull Name name, @NotNull ClassId enumClassId, @NotNull Name enumEntryName) {
            if (KotlinClass.KIND_CLASS_ID.equals(enumClassId) && KIND_FIELD_NAME.equals(name.asString())) {
                classKind = valueOfOrNull(KotlinClass.Kind.class, enumEntryName.asString());
            }
        }
    }

    private class PackageHeaderReader extends HeaderAnnotationArgumentVisitor {
        public PackageHeaderReader() {
            super(JvmClassName.byFqNameWithoutInnerClasses(KOTLIN_PACKAGE));
        }
    }

    private class FileFacadeHeaderReader extends HeaderAnnotationArgumentVisitor {
        public FileFacadeHeaderReader() {
            super(JvmClassName.byFqNameWithoutInnerClasses(KOTLIN_FILE_FACADE));
        }
    }

    private class MultifileClassHeaderReader extends HeaderAnnotationArgumentVisitor {
        public MultifileClassHeaderReader() {
            super(JvmClassName.byFqNameWithoutInnerClasses(KOTLIN_MULTIFILE_CLASS));
        }
    }

    private class MultifileClassPartHeaderReader extends HeaderAnnotationArgumentVisitor {
        public MultifileClassPartHeaderReader() {
            super(JvmClassName.byFqNameWithoutInnerClasses(KOTLIN_MULTIFILE_CLASS_PART));
        }
    }

    private class SyntheticClassHeaderReader extends HeaderAnnotationArgumentVisitor {
        public SyntheticClassHeaderReader() {
            super(KotlinSyntheticClass.CLASS_NAME);
        }

        @Override
        public void visitEnum(@NotNull Name name, @NotNull ClassId enumClassId, @NotNull Name enumEntryName) {
            if (KotlinSyntheticClass.KIND_CLASS_ID.equals(enumClassId) && KIND_FIELD_NAME.equals(name.asString())) {
                syntheticClassKind = valueOfOrNull(KotlinSyntheticClass.Kind.class, enumEntryName.asString());
            }
        }
    }

    // This function is needed here because Enum.valueOf() throws exception if there's no such value,
    // but we don't want to fail if we're loading the header with an _incompatible_ ABI version
    @Nullable
    private static <E extends Enum<E>> E valueOfOrNull(@NotNull Class<E> enumClass, @NotNull String entry) {
        try {
            return Enum.valueOf(enumClass, entry);
        }
        catch (IllegalArgumentException e) {
            return null;
        }
    }
}
