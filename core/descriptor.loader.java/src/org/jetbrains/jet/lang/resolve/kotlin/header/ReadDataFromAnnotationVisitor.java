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

import com.intellij.openapi.diagnostic.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.resolve.java.AbiVersionUtil;
import org.jetbrains.jet.lang.resolve.java.JvmAnnotationNames;
import org.jetbrains.jet.lang.resolve.java.JvmClassName;
import org.jetbrains.jet.lang.resolve.kotlin.KotlinJvmBinaryClass;
import org.jetbrains.jet.lang.resolve.name.Name;

import java.util.ArrayList;
import java.util.List;

import static org.jetbrains.jet.lang.resolve.java.AbiVersionUtil.isAbiVersionCompatible;
import static org.jetbrains.jet.lang.resolve.kotlin.KotlinJvmBinaryClass.AnnotationArgumentVisitor;
import static org.jetbrains.jet.lang.resolve.kotlin.KotlinJvmBinaryClass.AnnotationVisitor;

/* package */ class ReadDataFromAnnotationVisitor implements AnnotationVisitor {
    private static final Logger LOG = Logger.getInstance(ReadDataFromAnnotationVisitor.class);

    @SuppressWarnings("deprecation")
    private enum HeaderType {
        CLASS(JvmAnnotationNames.KOTLIN_CLASS),
        PACKAGE(JvmAnnotationNames.KOTLIN_PACKAGE),
        PACKAGE_FRAGMENT(JvmAnnotationNames.KOTLIN_PACKAGE_FRAGMENT),
        OLD_CLASS(JvmAnnotationNames.OLD_JET_CLASS_ANNOTATION),
        OLD_PACKAGE(JvmAnnotationNames.OLD_JET_PACKAGE_CLASS_ANNOTATION);

        @NotNull
        private final JvmClassName annotation;

        private HeaderType(@NotNull JvmClassName annotation) {
            this.annotation = annotation;
        }

        @Nullable
        private static HeaderType byClassName(@NotNull JvmClassName className) {
            for (HeaderType headerType : HeaderType.values()) {
                if (className.equals(headerType.annotation)) {
                    return headerType;
                }
            }
            return null;
        }
    }

    private int version = AbiVersionUtil.INVALID_VERSION;
    @Nullable
    private String[] annotationData = null;
    @Nullable
    private HeaderType foundType = null;

    @Nullable
    public KotlinClassFileHeader createHeader(@NotNull KotlinJvmBinaryClass kotlinClass) {
        if (foundType == null) {
            return null;
        }

        if (!AbiVersionUtil.isAbiVersionCompatible(version)) {
            return new IncompatibleAnnotationHeader(version);
        }

        switch (foundType) {
            case CLASS:
                return serializedDataHeader(SerializedDataHeader.Kind.CLASS, kotlinClass);
            case PACKAGE:
                return serializedDataHeader(SerializedDataHeader.Kind.PACKAGE, kotlinClass);
            case PACKAGE_FRAGMENT:
                return new PackageFragmentClassFileHeader(version);
            default:
                throw new UnsupportedOperationException("Unknown compatible HeaderType: " + foundType);
        }
    }

    @Nullable
    private SerializedDataHeader serializedDataHeader(@NotNull SerializedDataHeader.Kind kind, @NotNull KotlinJvmBinaryClass kotlinClass) {
        if (annotationData == null) {
            LOG.error("Kotlin annotation " + foundType + " is incorrect for class: " + kotlinClass);
            return null;
        }
        return new SerializedDataHeader(version, annotationData, kind);
    }

    @Nullable
    @Override
    public AnnotationArgumentVisitor visitAnnotation(@NotNull JvmClassName annotationClassName) {
        HeaderType newType = HeaderType.byClassName(annotationClassName);
        if (newType == null) return null;

        if (foundType != null) {
            LOG.error("Both annotations are present for compiled Kotlin file: " + foundType + " and " + newType);
            return null;
        }

        foundType = newType;

        if (newType == HeaderType.CLASS || newType == HeaderType.PACKAGE) {
            return kotlinClassOrPackageVisitor(annotationClassName);
        }
        else if (newType == HeaderType.PACKAGE_FRAGMENT) {
            return kotlinPackageFragmentVisitor(annotationClassName);
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
    private AnnotationArgumentVisitor kotlinPackageFragmentVisitor(@NotNull final JvmClassName annotationClassName) {
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
