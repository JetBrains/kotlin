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

package org.jetbrains.jet.lang.resolve.kotlin.header;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.resolve.java.AbiVersionUtil;
import org.jetbrains.jet.lang.resolve.java.JvmClassName;
import org.jetbrains.kotlin.name.ClassId;
import org.jetbrains.kotlin.name.Name;

import java.util.*;

import static org.jetbrains.jet.lang.resolve.java.AbiVersionUtil.isAbiVersionCompatible;
import static org.jetbrains.jet.lang.resolve.java.JvmAnnotationNames.*;
import static org.jetbrains.jet.lang.resolve.kotlin.KotlinJvmBinaryClass.*;
import static org.jetbrains.jet.lang.resolve.kotlin.header.KotlinClassHeader.Kind.*;

public class ReadKotlinClassHeaderAnnotationVisitor implements AnnotationVisitor {
    private static final Map<JvmClassName, KotlinClassHeader.Kind> HEADER_KINDS = new HashMap<JvmClassName, KotlinClassHeader.Kind>();
    private static final Map<JvmClassName, KotlinClassHeader.Kind> OLD_DEPRECATED_ANNOTATIONS_KINDS = new HashMap<JvmClassName, KotlinClassHeader.Kind>();

    private int version = AbiVersionUtil.INVALID_VERSION;
    static {
        HEADER_KINDS.put(JvmClassName.byFqNameWithoutInnerClasses(KOTLIN_CLASS), CLASS);
        HEADER_KINDS.put(JvmClassName.byFqNameWithoutInnerClasses(KOTLIN_PACKAGE), PACKAGE_FACADE);
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

    private String[] annotationData = null;
    private KotlinClassHeader.Kind headerKind = null;
    private KotlinSyntheticClass.Kind syntheticClassKind = null;

    @Nullable
    public KotlinClassHeader createHeader() {
        if (headerKind == null) {
            return null;
        }

        if (!AbiVersionUtil.isAbiVersionCompatible(version)) {
            return new KotlinClassHeader(headerKind, version, null, syntheticClassKind);
        }

        if ((headerKind == CLASS || headerKind == PACKAGE_FACADE) && annotationData == null) {
            // This means that the annotation is found and its ABI version is compatible, but there's no "data" string array in it.
            // We tell the outside world that there's really no annotation at all
            return null;
        }

        return new KotlinClassHeader(headerKind, version, annotationData, syntheticClassKind);
    }

    @Nullable
    @Override
    public AnnotationArgumentVisitor visitAnnotation(@NotNull ClassId classId) {
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
                    return kotlinClassOrPackageVisitor(annotation);
                case PACKAGE_FACADE:
                    return kotlinClassOrPackageVisitor(annotation);
                case SYNTHETIC_CLASS:
                    return syntheticClassAnnotationVisitor();
                default:
                    throw new IllegalStateException("Unknown kind: " + newKind);
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

    @NotNull
    private AnnotationArgumentVisitor kotlinClassOrPackageVisitor(@NotNull final JvmClassName annotationClassName) {
        return new AnnotationArgumentVisitor() {
            @Override
            public void visit(@Nullable Name name, @Nullable Object value) {
                visitIntValueForSupportedAnnotation(name, value, annotationClassName);
            }

            @Override
            public void visitEnum(@NotNull Name name, @NotNull ClassId enumClassId, @NotNull Name enumEntryName) {
                unexpectedArgument(name, annotationClassName);
            }

            @Override
            @Nullable
            public AnnotationArrayArgumentVisitor visitArray(@NotNull Name name) {
                if (name.asString().equals(DATA_FIELD_NAME)) {
                    return stringArrayVisitor();
                }
                else if (isAbiVersionCompatible(version)) {
                    throw new IllegalStateException("Unexpected array argument " + name + " for annotation " + annotationClassName);
                }

                return null;
            }

            @NotNull
            private AnnotationArrayArgumentVisitor stringArrayVisitor() {
                final List<String> strings = new ArrayList<String>(1);
                return new AnnotationArrayArgumentVisitor() {
                    @Override
                    public void visit(@Nullable Object value) {
                        if (!(value instanceof String)) {
                            throw new IllegalStateException("Unexpected argument value: " + value);
                        }

                        strings.add((String) value);
                    }

                    @Override
                    public void visitEnum(@NotNull ClassId enumClassId, @NotNull Name enumEntryName) {
                        unexpectedArgument(null, annotationClassName);
                    }

                    @Override
                    public void visitEnd() {
                        //noinspection SSBasedInspection
                        annotationData = strings.toArray(new String[strings.size()]);
                    }
                };
            }

            @Override
            public void visitEnd() {
            }
        };
    }

    @NotNull
    private AnnotationArgumentVisitor syntheticClassAnnotationVisitor() {
        return new AnnotationArgumentVisitor() {
            @Override
            public void visit(@Nullable Name name, @Nullable Object value) {
                visitIntValueForSupportedAnnotation(name, value, KotlinSyntheticClass.CLASS_NAME);
            }

            @Override
            public void visitEnum(@NotNull Name name, @NotNull ClassId enumClassId, @NotNull Name enumEntryName) {
                if (enumClassId.equals(KotlinSyntheticClass.KIND_CLASS_ID) && name.equals(KotlinSyntheticClass.KIND_FIELD_NAME)) {
                    // Don't call KotlinSyntheticClass.Kind.valueOf() here, because it will throw an exception if there's no such value,
                    // but we don't want to fail if we're loading the header with an _incompatible_ ABI version
                    syntheticClassKind = KotlinSyntheticClass.Kind.valueOfOrNull(enumEntryName.asString());
                    if (syntheticClassKind != null) return;
                }
                if (isAbiVersionCompatible(version)) {
                    throw new IllegalStateException("Unexpected enum entry for synthetic class annotation: " +
                                                    name + "=" + enumClassId + "." + enumEntryName);
                }
            }

            @Nullable
            @Override
            public AnnotationArrayArgumentVisitor visitArray(@NotNull Name name) {
                return unexpectedArgument(name, KotlinSyntheticClass.CLASS_NAME);
            }

            @Override
            public void visitEnd() {
            }
        };
    }

    private void visitIntValueForSupportedAnnotation(@Nullable Name name, @Nullable Object value, @NotNull JvmClassName className) {
        if (name != null && name.asString().equals(ABI_VERSION_FIELD_NAME)) {
            version = value == null ? AbiVersionUtil.INVALID_VERSION : (Integer) value;
        }
        else {
            unexpectedArgument(name, className);
        }
    }

    @Nullable
    private AnnotationArrayArgumentVisitor unexpectedArgument(@Nullable Name name, @NotNull JvmClassName annotationClassName) {
        if (isAbiVersionCompatible(version)) {
            throw new IllegalStateException("Unexpected argument " + name + " for annotation " + annotationClassName);
        }
        return null;
    }
}
