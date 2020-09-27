/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.findUsages

import com.intellij.usages.impl.rules.UsageType
import org.jetbrains.kotlin.idea.KotlinBundleIndependent

object KotlinUsageTypes {

    internal fun UsageTypeEnum.toUsageType(): UsageType = when (this) {
        UsageTypeEnum.TYPE_CONSTRAINT -> TYPE_CONSTRAINT
        UsageTypeEnum.VALUE_PARAMETER_TYPE -> VALUE_PARAMETER_TYPE
        UsageTypeEnum.NON_LOCAL_PROPERTY_TYPE -> NON_LOCAL_PROPERTY_TYPE
        UsageTypeEnum.FUNCTION_RETURN_TYPE -> FUNCTION_RETURN_TYPE
        UsageTypeEnum.SUPER_TYPE -> SUPER_TYPE
        UsageTypeEnum.IS -> IS
        UsageTypeEnum.CLASS_OBJECT_ACCESS -> CLASS_OBJECT_ACCESS
        UsageTypeEnum.COMPANION_OBJECT_ACCESS -> COMPANION_OBJECT_ACCESS
        UsageTypeEnum.EXTENSION_RECEIVER_TYPE -> EXTENSION_RECEIVER_TYPE
        UsageTypeEnum.SUPER_TYPE_QUALIFIER -> SUPER_TYPE_QUALIFIER
        UsageTypeEnum.TYPE_ALIAS -> TYPE_ALIAS

        UsageTypeEnum.FUNCTION_CALL -> FUNCTION_CALL
        UsageTypeEnum.IMPLICIT_GET -> IMPLICIT_GET
        UsageTypeEnum.IMPLICIT_SET -> IMPLICIT_SET
        UsageTypeEnum.IMPLICIT_INVOKE -> IMPLICIT_INVOKE
        UsageTypeEnum.IMPLICIT_ITERATION -> IMPLICIT_ITERATION
        UsageTypeEnum.PROPERTY_DELEGATION -> PROPERTY_DELEGATION

        UsageTypeEnum.RECEIVER -> RECEIVER
        UsageTypeEnum.DELEGATE -> DELEGATE

        UsageTypeEnum.PACKAGE_DIRECTIVE -> PACKAGE_DIRECTIVE
        UsageTypeEnum.PACKAGE_MEMBER_ACCESS -> PACKAGE_MEMBER_ACCESS

        UsageTypeEnum.CALLABLE_REFERENCE -> CALLABLE_REFERENCE

        UsageTypeEnum.READ -> UsageType.READ
        UsageTypeEnum.WRITE -> UsageType.WRITE
        UsageTypeEnum.CLASS_IMPORT -> UsageType.CLASS_IMPORT
        UsageTypeEnum.CLASS_LOCAL_VAR_DECLARATION -> UsageType.CLASS_LOCAL_VAR_DECLARATION
        UsageTypeEnum.TYPE_PARAMETER -> UsageType.TYPE_PARAMETER
        UsageTypeEnum.CLASS_CAST_TO -> UsageType.CLASS_CAST_TO
        UsageTypeEnum.ANNOTATION -> UsageType.ANNOTATION
        UsageTypeEnum.CLASS_NEW_OPERATOR -> UsageType.CLASS_NEW_OPERATOR
        UsageTypeEnum.NAMED_ARGUMENT -> NAMED_ARGUMENT

        UsageTypeEnum.USAGE_IN_STRING_LITERAL -> UsageType.LITERAL_USAGE
    }

    // types
    val TYPE_CONSTRAINT = UsageType(KotlinBundleIndependent.lazyMessage("find.usages.type.type.constraint"))
    val VALUE_PARAMETER_TYPE = UsageType(KotlinBundleIndependent.lazyMessage("find.usages.type.value.parameter.type"))
    val NON_LOCAL_PROPERTY_TYPE = UsageType(KotlinBundleIndependent.lazyMessage("find.usages.type.nonLocal.property.type"))
    val FUNCTION_RETURN_TYPE = UsageType(KotlinBundleIndependent.lazyMessage("find.usages.type.function.return.type"))
    val SUPER_TYPE = UsageType(KotlinBundleIndependent.lazyMessage("find.usages.type.superType"))
    val IS = UsageType(KotlinBundleIndependent.lazyMessage("find.usages.type.is"))
    val CLASS_OBJECT_ACCESS = UsageType(KotlinBundleIndependent.lazyMessage("find.usages.type.class.object"))
    val COMPANION_OBJECT_ACCESS = UsageType(KotlinBundleIndependent.lazyMessage("find.usages.type.companion.object"))
    val EXTENSION_RECEIVER_TYPE = UsageType(KotlinBundleIndependent.lazyMessage("find.usages.type.extension.receiver.type"))
    val SUPER_TYPE_QUALIFIER = UsageType(KotlinBundleIndependent.lazyMessage("find.usages.type.super.type.qualifier"))
    val TYPE_ALIAS = UsageType(KotlinBundleIndependent.lazyMessage("find.usages.type.type.alias"))

    // functions
    val FUNCTION_CALL = UsageType(KotlinBundleIndependent.lazyMessage("find.usages.type.function.call"))
    val IMPLICIT_GET = UsageType(KotlinBundleIndependent.lazyMessage("find.usages.type.implicit.get"))
    val IMPLICIT_SET = UsageType(KotlinBundleIndependent.lazyMessage("find.usages.type.implicit.set"))
    val IMPLICIT_INVOKE = UsageType(KotlinBundleIndependent.lazyMessage("find.usages.type.implicit.invoke"))
    val IMPLICIT_ITERATION = UsageType(KotlinBundleIndependent.lazyMessage("find.usages.type.implicit.iteration"))
    val PROPERTY_DELEGATION = UsageType(KotlinBundleIndependent.lazyMessage("find.usages.type.property.delegation"))

    // values
    val RECEIVER = UsageType(KotlinBundleIndependent.lazyMessage("find.usages.type.receiver"))
    val DELEGATE = UsageType(KotlinBundleIndependent.lazyMessage("find.usages.type.delegate"))

    // packages
    val PACKAGE_DIRECTIVE = UsageType(KotlinBundleIndependent.lazyMessage("find.usages.type.packageDirective"))
    val PACKAGE_MEMBER_ACCESS = UsageType(KotlinBundleIndependent.lazyMessage("find.usages.type.packageMemberAccess"))

    // common usage types
    val CALLABLE_REFERENCE = UsageType(KotlinBundleIndependent.lazyMessage("find.usages.type.callable.reference"))
    val NAMED_ARGUMENT = UsageType(KotlinBundleIndependent.lazyMessage("find.usages.type.named.argument"))
}
