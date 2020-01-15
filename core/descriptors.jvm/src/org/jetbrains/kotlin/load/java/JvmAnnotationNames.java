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

package org.jetbrains.kotlin.load.java;

import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.resolve.jvm.JvmClassName;

@SuppressWarnings("PointlessBitwiseExpression")
public final class JvmAnnotationNames {
    public static final FqName METADATA_FQ_NAME = new FqName("kotlin.Metadata");
    public static final String METADATA_DESC = "L" + JvmClassName.byFqNameWithoutInnerClasses(METADATA_FQ_NAME).getInternalName() + ";";

    public static final String METADATA_VERSION_FIELD_NAME = "mv";
    public static final String BYTECODE_VERSION_FIELD_NAME = "bv";
    public static final String KIND_FIELD_NAME = "k";
    public static final String METADATA_DATA_FIELD_NAME = "d1";
    public static final String METADATA_STRINGS_FIELD_NAME = "d2";
    public static final String METADATA_EXTRA_STRING_FIELD_NAME = "xs";
    public static final String METADATA_PACKAGE_NAME_FIELD_NAME = "pn";
    public static final String METADATA_MULTIFILE_CLASS_NAME_FIELD_NAME = METADATA_EXTRA_STRING_FIELD_NAME;
    public static final String METADATA_EXTRA_INT_FIELD_NAME = "xi";

    public static final int METADATA_MULTIFILE_PARTS_INHERIT_FLAG = 1 << 0;
    public static final int METADATA_PRE_RELEASE_FLAG = 1 << 1;
    public static final int METADATA_SCRIPT_FLAG = 1 << 2;
    public static final int METADATA_STRICT_VERSION_SEMANTICS_FLAG = 1 << 3;
    public static final int METADATA_JVM_IR_FLAG = 1 << 4;
    public static final int METADATA_JVM_IR_STABLE_ABI_FLAG = 1 << 5;

    public static final Name DEFAULT_ANNOTATION_MEMBER_NAME = Name.identifier("value");

    public static final FqName JETBRAINS_NOT_NULL_ANNOTATION = new FqName("org.jetbrains.annotations.NotNull");
    public static final FqName JETBRAINS_NULLABLE_ANNOTATION = new FqName("org.jetbrains.annotations.Nullable");
    public static final FqName JETBRAINS_MUTABLE_ANNOTATION = new FqName("org.jetbrains.annotations.Mutable");
    public static final FqName JETBRAINS_READONLY_ANNOTATION = new FqName("org.jetbrains.annotations.ReadOnly");

    public static final FqName READONLY_ANNOTATION = new FqName("kotlin.annotations.jvm.ReadOnly");
    public static final FqName MUTABLE_ANNOTATION = new FqName("kotlin.annotations.jvm.Mutable");

    public static final FqName PURELY_IMPLEMENTS_ANNOTATION = new FqName("kotlin.jvm.PurelyImplements");

    // Just for internal use: there is no such real classes in bytecode
    public static final FqName ENHANCED_NULLABILITY_ANNOTATION = new FqName("kotlin.jvm.internal.EnhancedNullability");
    public static final FqName ENHANCED_MUTABILITY_ANNOTATION = new FqName("kotlin.jvm.internal.EnhancedMutability");

    public static final FqName PARAMETER_NAME_FQ_NAME = new FqName("kotlin.annotations.jvm.internal.ParameterName");
    public static final FqName DEFAULT_VALUE_FQ_NAME = new FqName("kotlin.annotations.jvm.internal.DefaultValue");
    public static final FqName DEFAULT_NULL_FQ_NAME = new FqName("kotlin.annotations.jvm.internal.DefaultNull");

    private JvmAnnotationNames() {
    }
}
