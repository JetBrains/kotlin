/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.load.java;

import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.resolve.jvm.JvmClassName;

import java.lang.annotation.*;

@SuppressWarnings("PointlessBitwiseExpression")
public final class JvmAnnotationNames {
    public static final FqName METADATA_FQ_NAME = new FqName("kotlin.Metadata");
    public static final String METADATA_DESC = "L" + JvmClassName.byFqNameWithoutInnerClasses(METADATA_FQ_NAME).getInternalName() + ";";

    public static final String METADATA_VERSION_FIELD_NAME = "mv";
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
    @SuppressWarnings("unused")
    public static final int METADATA_FIR_FLAG = 1 << 6;
    public static final int METADATA_PUBLIC_ABI_FLAG = 1 << 7;

    public static final Name DEFAULT_ANNOTATION_MEMBER_NAME = Name.identifier("value");

    public static final FqName TARGET_ANNOTATION = new FqName(Target.class.getName());
    public static final FqName ELEMENT_TYPE_ENUM = new FqName(ElementType.class.getName());
    public static final FqName RETENTION_ANNOTATION = new FqName(Retention.class.getName());
    public static final FqName RETENTION_POLICY_ENUM = new FqName(RetentionPolicy.class.getName());
    public static final FqName DEPRECATED_ANNOTATION = new FqName(Deprecated.class.getName());
    public static final FqName DOCUMENTED_ANNOTATION = new FqName(Documented.class.getName());
    public static final FqName REPEATABLE_ANNOTATION = new FqName("java.lang.annotation.Repeatable");
    public static final FqName OVERRIDE_ANNOTATION = new FqName(Override.class.getName());

    public static final FqName JETBRAINS_NOT_NULL_ANNOTATION = new FqName("org.jetbrains.annotations.NotNull");
    public static final FqName JETBRAINS_NULLABLE_ANNOTATION = new FqName("org.jetbrains.annotations.Nullable");
    public static final FqName JETBRAINS_MUTABLE_ANNOTATION = new FqName("org.jetbrains.annotations.Mutable");
    public static final FqName JETBRAINS_READONLY_ANNOTATION = new FqName("org.jetbrains.annotations.ReadOnly");

    public static final FqName READONLY_ANNOTATION = new FqName("kotlin.annotations.jvm.ReadOnly");
    public static final FqName MUTABLE_ANNOTATION = new FqName("kotlin.annotations.jvm.Mutable");

    public static final FqName PURELY_IMPLEMENTS_ANNOTATION = new FqName("kotlin.jvm.PurelyImplements");

    public static final FqName KOTLIN_JVM_INTERNAL = new FqName("kotlin.jvm.internal");

    public static final FqName SERIALIZED_IR_FQ_NAME = new FqName("kotlin.jvm.internal.SerializedIr");
    public static final String SERIALIZED_IR_DESC = "L" + JvmClassName.byFqNameWithoutInnerClasses(SERIALIZED_IR_FQ_NAME).getInternalName() + ";";
    public static final String SERIALIZED_IR_BYTES_FIELD_NAME = "b";

    public static final String SOURCE_DEBUG_EXTENSION_DESC = "Lkotlin/jvm/internal/SourceDebugExtension;";

    // Just for internal use: there is no such real classes in bytecode
    public static final FqName ENHANCED_NULLABILITY_ANNOTATION = new FqName("kotlin.jvm.internal.EnhancedNullability");
    public static final FqName ENHANCED_MUTABILITY_ANNOTATION = new FqName("kotlin.jvm.internal.EnhancedMutability");

    private JvmAnnotationNames() {
    }
}
