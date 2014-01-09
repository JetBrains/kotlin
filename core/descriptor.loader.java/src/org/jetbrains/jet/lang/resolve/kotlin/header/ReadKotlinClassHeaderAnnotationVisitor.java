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
import org.jetbrains.jet.lang.resolve.java.JvmAnnotationNames;
import org.jetbrains.jet.lang.resolve.java.JvmClassName;
import org.jetbrains.jet.lang.resolve.kotlin.KotlinJvmBinaryClass;
import org.jetbrains.jet.lang.resolve.name.Name;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.jetbrains.jet.lang.resolve.java.AbiVersionUtil.isAbiVersionCompatible;
import static org.jetbrains.jet.lang.resolve.java.JvmAnnotationNames.*;
import static org.jetbrains.jet.lang.resolve.kotlin.KotlinJvmBinaryClass.AnnotationArgumentVisitor;
import static org.jetbrains.jet.lang.resolve.kotlin.KotlinJvmBinaryClass.AnnotationVisitor;
import static org.jetbrains.jet.lang.resolve.kotlin.header.KotlinClassHeader.Kind.*;

public class ReadKotlinClassHeaderAnnotationVisitor implements AnnotationVisitor {

    private static final Map<JvmClassName, KotlinClassHeader.Kind> HEADER_KINDS = new HashMap<JvmClassName, KotlinClassHeader.Kind>();
    static {
        HEADER_KINDS.put(JvmClassName.byFqNameWithoutInnerClasses(KOTLIN_CLASS), CLASS);
        HEADER_KINDS.put(JvmClassName.byFqNameWithoutInnerClasses(KOTLIN_PACKAGE), PACKAGE_FACADE);
        HEADER_KINDS.put(JvmClassName.byFqNameWithoutInnerClasses(KOTLIN_PACKAGE_FRAGMENT), PACKAGE_FRAGMENT);
        HEADER_KINDS.put(JvmClassName.byFqNameWithoutInnerClasses(KOTLIN_TRAIT_IMPL), TRAIT_IMPL);
        HEADER_KINDS.put(JvmClassName.byFqNameWithoutInnerClasses(OLD_JET_CLASS_ANNOTATION), INCOMPATIBLE_ABI_VERSION);
        HEADER_KINDS.put(JvmClassName.byFqNameWithoutInnerClasses(OLD_JET_PACKAGE_CLASS_ANNOTATION), INCOMPATIBLE_ABI_VERSION);
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
            return KotlinClassHeader.createIncompatibleVersionErrorHeader(version);
        }

        switch (headerKind) {
            case CLASS:
                // This means that the annotation is found and its ABI version is compatible, but there's no "data" string array in it.
                // We tell the outside world that there's really no annotation at all
                if (annotationData == null) return null;
                return KotlinClassHeader.createClassHeader(version, annotationData);
            case PACKAGE_FACADE:
                if (annotationData == null) return null;
                return KotlinClassHeader.createPackageFacadeHeader(version, annotationData);
            case PACKAGE_FRAGMENT:
                return KotlinClassHeader.createPackageFragmentHeader(version);
            case TRAIT_IMPL:
                return KotlinClassHeader.createTraitImplHeader(version);
            default:
                throw new UnsupportedOperationException("Unknown compatible HeaderType: " + headerKind);
        }
    }

    @Nullable
    @Override
    public AnnotationArgumentVisitor visitAnnotation(@NotNull JvmClassName annotationClassName) {
        KotlinClassHeader.Kind newKind = HEADER_KINDS.get(annotationClassName);
        if (newKind == null) return null;

        if (this.headerKind != null) {
            // Ignore all Kotlin annotations except the first found
            return null;
        }

        headerKind = newKind;

        if (newKind == CLASS || newKind == PACKAGE_FACADE) {
            return kotlinClassOrPackageVisitor(annotationClassName);
        }
        else if (newKind == PACKAGE_FRAGMENT || newKind == TRAIT_IMPL) {
            return annotationWithAbiVersionVisitor(annotationClassName);
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
                if (name.asString().equals(JvmAnnotationNames.DATA_FIELD_NAME)) {
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
    private AnnotationArgumentVisitor annotationWithAbiVersionVisitor(@NotNull final JvmClassName annotationClassName) {
        return new AnnotationArgumentVisitor() {
            @Override
            public void visit(@Nullable Name name, @Nullable Object value) {
                visitIntValueForSupportedAnnotation(name, value, annotationClassName);
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
            }
        };
    }

    private void visitIntValueForSupportedAnnotation(@Nullable Name name, @Nullable Object value, @NotNull JvmClassName className) {
        if (name != null && name.asString().equals(JvmAnnotationNames.ABI_VERSION_FIELD_NAME)) {
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
