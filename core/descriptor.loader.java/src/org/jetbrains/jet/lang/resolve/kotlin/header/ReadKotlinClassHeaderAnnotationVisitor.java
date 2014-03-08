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
import org.jetbrains.jet.lang.resolve.kotlin.KotlinJvmBinaryClass;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.Name;

import java.util.*;

import static org.jetbrains.jet.lang.resolve.java.AbiVersionUtil.isAbiVersionCompatible;
import static org.jetbrains.jet.lang.resolve.java.JvmAnnotationNames.*;
import static org.jetbrains.jet.lang.resolve.kotlin.KotlinJvmBinaryClass.AnnotationArgumentVisitor;
import static org.jetbrains.jet.lang.resolve.kotlin.KotlinJvmBinaryClass.AnnotationVisitor;
import static org.jetbrains.jet.lang.resolve.kotlin.header.KotlinClassHeader.Kind.*;

public class ReadKotlinClassHeaderAnnotationVisitor implements AnnotationVisitor {
    private static final JvmClassName KOTLIN_CLASS_ANNOTATION = JvmClassName.byFqNameWithoutInnerClasses(KOTLIN_CLASS);
    private static final JvmClassName KOTLIN_PACKAGE_ANNOTATION = JvmClassName.byFqNameWithoutInnerClasses(KOTLIN_PACKAGE);
    private static final JvmClassName KOTLIN_SYNTHETIC_CLASS_ANNOTATION = JvmClassName.byInternalName(KotlinSyntheticClass.INTERNAL_NAME);
    private static final Set<JvmClassName> ALL_SUPPORTED_ANNOTATIONS = new HashSet<JvmClassName>();

    static {
        ALL_SUPPORTED_ANNOTATIONS.add(KOTLIN_CLASS_ANNOTATION);
        ALL_SUPPORTED_ANNOTATIONS.add(KOTLIN_PACKAGE_ANNOTATION);
        ALL_SUPPORTED_ANNOTATIONS.add(KOTLIN_SYNTHETIC_CLASS_ANNOTATION);

        @SuppressWarnings("deprecation")
        List<FqName> incompatible = Arrays.asList(OLD_JET_CLASS_ANNOTATION, OLD_JET_PACKAGE_CLASS_ANNOTATION, OLD_KOTLIN_CLASS,
                                                  OLD_KOTLIN_PACKAGE, OLD_KOTLIN_PACKAGE_FRAGMENT, OLD_KOTLIN_TRAIT_IMPL);
        for (FqName fqName : incompatible) {
            ALL_SUPPORTED_ANNOTATIONS.add(JvmClassName.byFqNameWithoutInnerClasses(fqName));
        }
    }

    private int version = AbiVersionUtil.INVALID_VERSION;
    @Nullable
    private String[] annotationData = null;
    @Nullable
    private KotlinClassHeader.Kind headerKind = null;

    private ReadKotlinClassHeaderAnnotationVisitor() {
    }

    @Nullable
    public static KotlinClassHeader read(@NotNull KotlinJvmBinaryClass kotlinClass) {
        ReadKotlinClassHeaderAnnotationVisitor visitor = new ReadKotlinClassHeaderAnnotationVisitor();
        kotlinClass.loadClassAnnotations(visitor);
        return visitor.createHeader();
    }

    @Nullable
    public KotlinClassHeader createHeader() {
        if (headerKind == null) {
            return null;
        }

        if (!AbiVersionUtil.isAbiVersionCompatible(version)) {
            return new KotlinClassHeader(INCOMPATIBLE_ABI_VERSION, version, null);
        }

        if ((headerKind == CLASS || headerKind == PACKAGE_FACADE) && annotationData == null) {
            // This means that the annotation is found and its ABI version is compatible, but there's no "data" string array in it.
            // We tell the outside world that there's really no annotation at all
            return null;
        }
        return new KotlinClassHeader(headerKind, version, annotationData);
    }

    @Nullable
    @Override
    public AnnotationArgumentVisitor visitAnnotation(@NotNull JvmClassName annotation) {
        if (!ALL_SUPPORTED_ANNOTATIONS.contains(annotation)) return null;

        if (headerKind != null) {
            // Ignore all Kotlin annotations except the first found
            return null;
        }

        if (annotation.equals(KOTLIN_CLASS_ANNOTATION) || annotation.equals(KOTLIN_PACKAGE_ANNOTATION)) {
            headerKind = annotation.equals(KOTLIN_CLASS_ANNOTATION) ? CLASS : PACKAGE_FACADE;
            return kotlinClassOrPackageVisitor(annotation);
        }
        else if (annotation.equals(KOTLIN_SYNTHETIC_CLASS_ANNOTATION)) {
            return syntheticClassAnnotationVisitor();
        }
        else {
            headerKind = INCOMPATIBLE_ABI_VERSION;
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
            public void visitEnum(@NotNull Name name, @NotNull JvmClassName enumClassName, @NotNull Name enumEntryName) {
                unexpectedArgument(name, annotationClassName);
            }

            @Override
            @Nullable
            public AnnotationArgumentVisitor visitArray(@NotNull Name name) {
                if (name.asString().equals(DATA_FIELD_NAME)) {
                    return stringArrayVisitor();
                }
                else if (isAbiVersionCompatible(version)) {
                    throw new IllegalStateException("Unexpected array argument " + name + " for annotation " + annotationClassName);
                }

                return null;
            }

            @NotNull
            private AnnotationArgumentVisitor stringArrayVisitor() {
                final List<String> strings = new ArrayList<String>(1);
                return new AnnotationArgumentVisitor() {
                    @Override
                    public void visit(@Nullable Name name, @Nullable Object value) {
                        if (!(value instanceof String)) {
                            throw new IllegalStateException("Unexpected argument value: " + value);
                        }

                        strings.add((String) value);
                    }

                    @Override
                    public void visitEnum(@NotNull Name name, @NotNull JvmClassName enumClassName, @NotNull Name enumEntryName) {
                        unexpectedArgument(name, annotationClassName);
                    }

                    @Nullable
                    @Override
                    public AnnotationArgumentVisitor visitArray(@NotNull Name name) {
                        return unexpectedArgument(name, annotationClassName);
                    }

                    @Override
                    public void visitEnd() {
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
                visitIntValueForSupportedAnnotation(name, value, KOTLIN_SYNTHETIC_CLASS_ANNOTATION);
            }

            @Override
            public void visitEnum(@NotNull Name name, @NotNull JvmClassName enumClassName, @NotNull Name enumEntryName) {
                if (enumClassName.getInternalName().equals(KotlinSyntheticClass.KIND_INTERNAL_NAME) &&
                    name.equals(KotlinSyntheticClass.KIND_FIELD_NAME)) {
                    String value = enumEntryName.asString();
                    // Don't call KotlinSyntheticClass.Kind.valueOf() here, because it will throw an exception if there's no such value,
                    // but we don't want to fail if we're loading the header with an _incompatible_ ABI version
                    if (value.equals(KotlinSyntheticClass.Kind.PACKAGE_PART.toString())) {
                        headerKind = PACKAGE_PART;
                        return;
                    }
                    else if (value.equals(KotlinSyntheticClass.Kind.TRAIT_IMPL.toString())) {
                        headerKind = TRAIT_IMPL;
                        return;
                    }
                }
                if (isAbiVersionCompatible(version)) {
                    throw new IllegalStateException("Unexpected enum entry for synthetic class annotation: " +
                                                    name + "=" + enumClassName + "." + enumEntryName);
                }
            }

            @Nullable
            @Override
            public AnnotationArgumentVisitor visitArray(@NotNull Name name) {
                return unexpectedArgument(name, KOTLIN_SYNTHETIC_CLASS_ANNOTATION);
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
    private AnnotationArgumentVisitor unexpectedArgument(@Nullable Name name, @NotNull JvmClassName annotationClassName) {
        if (isAbiVersionCompatible(version)) {
            throw new IllegalStateException("Unexpected argument " + name + " for annotation " + annotationClassName);
        }
        return null;
    }
}
