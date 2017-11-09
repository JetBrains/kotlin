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

package org.jetbrains.kotlin.idea.findUsages

import com.intellij.psi.PsiElement
import com.intellij.usages.UsageTarget
import com.intellij.usages.impl.rules.UsageType
import com.intellij.usages.impl.rules.UsageTypeProviderEx
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.findUsages.UsageTypeEnum.*

object KotlinUsageTypeProvider : UsageTypeProviderEx {
    override fun getUsageType(element: PsiElement?): UsageType? = getUsageType(element, UsageTarget.EMPTY_ARRAY)

    override fun getUsageType(element: PsiElement?, targets: Array<out UsageTarget>): UsageType? {
        val usageType = UsageTypeUtils.getUsageType(element) ?: return null
        return convertEnumToUsageType(usageType)
    }

    fun convertEnumToUsageType(usageType: UsageTypeEnum): UsageType = when (usageType) {
        TYPE_CONSTRAINT -> KotlinUsageTypes.TYPE_CONSTRAINT
        VALUE_PARAMETER_TYPE -> KotlinUsageTypes.VALUE_PARAMETER_TYPE
        NON_LOCAL_PROPERTY_TYPE -> KotlinUsageTypes.NON_LOCAL_PROPERTY_TYPE
        FUNCTION_RETURN_TYPE -> KotlinUsageTypes.FUNCTION_RETURN_TYPE
        SUPER_TYPE -> KotlinUsageTypes.SUPER_TYPE
        IS -> KotlinUsageTypes.IS
        CLASS_OBJECT_ACCESS -> KotlinUsageTypes.CLASS_OBJECT_ACCESS
        COMPANION_OBJECT_ACCESS -> KotlinUsageTypes.COMPANION_OBJECT_ACCESS
        EXTENSION_RECEIVER_TYPE -> KotlinUsageTypes.EXTENSION_RECEIVER_TYPE
        SUPER_TYPE_QUALIFIER -> KotlinUsageTypes.SUPER_TYPE_QUALIFIER
        TYPE_ALIAS -> KotlinUsageTypes.TYPE_ALIAS

        FUNCTION_CALL -> KotlinUsageTypes.FUNCTION_CALL
        IMPLICIT_GET -> KotlinUsageTypes.IMPLICIT_GET
        IMPLICIT_SET -> KotlinUsageTypes.IMPLICIT_SET
        IMPLICIT_INVOKE -> KotlinUsageTypes.IMPLICIT_INVOKE
        IMPLICIT_ITERATION -> KotlinUsageTypes.IMPLICIT_ITERATION
        PROPERTY_DELEGATION -> KotlinUsageTypes.PROPERTY_DELEGATION

        RECEIVER -> KotlinUsageTypes.RECEIVER
        DELEGATE -> KotlinUsageTypes.DELEGATE

        PACKAGE_DIRECTIVE -> KotlinUsageTypes.PACKAGE_DIRECTIVE
        PACKAGE_MEMBER_ACCESS -> KotlinUsageTypes.PACKAGE_MEMBER_ACCESS

        CALLABLE_REFERENCE -> KotlinUsageTypes.CALLABLE_REFERENCE

        READ -> UsageType.READ
        WRITE -> UsageType.WRITE
        CLASS_IMPORT -> UsageType.CLASS_IMPORT
        CLASS_LOCAL_VAR_DECLARATION -> UsageType.CLASS_LOCAL_VAR_DECLARATION
        TYPE_PARAMETER -> UsageType.TYPE_PARAMETER
        CLASS_CAST_TO -> UsageType.CLASS_CAST_TO
        ANNOTATION -> UsageType.ANNOTATION
        CLASS_NEW_OPERATOR -> UsageType.CLASS_NEW_OPERATOR
        NAMED_ARGUMENT -> KotlinUsageTypes.NAMED_ARGUMENT

        USAGE_IN_STRING_LITERAL -> UsageType.LITERAL_USAGE
    }
}

object KotlinUsageTypes {
    // types
    val TYPE_CONSTRAINT = UsageType(KotlinBundle.message("usageType.type.constraint"))
    val VALUE_PARAMETER_TYPE = UsageType(KotlinBundle.message("usageType.value.parameter.type"))
    val NON_LOCAL_PROPERTY_TYPE = UsageType(KotlinBundle.message("usageType.nonLocal.property.type"))
    val FUNCTION_RETURN_TYPE = UsageType(KotlinBundle.message("usageType.function.return.type"))
    val SUPER_TYPE = UsageType(KotlinBundle.message("usageType.superType"))
    val IS = UsageType(KotlinBundle.message("usageType.is"))
    val CLASS_OBJECT_ACCESS = UsageType(KotlinBundle.message("usageType.class.object"))
    val COMPANION_OBJECT_ACCESS = UsageType(KotlinBundle.message("usageType.companion.object"))
    val EXTENSION_RECEIVER_TYPE = UsageType(KotlinBundle.message("usageType.extension.receiver.type"))
    val SUPER_TYPE_QUALIFIER = UsageType(KotlinBundle.message("usageType.super.type.qualifier"))
    val TYPE_ALIAS = UsageType("Type alias")

    // functions
    val FUNCTION_CALL = UsageType(KotlinBundle.message("usageType.function.call"))
    val IMPLICIT_GET = UsageType(KotlinBundle.message("usageType.implicit.get"))
    val IMPLICIT_SET = UsageType(KotlinBundle.message("usageType.implicit.set"))
    val IMPLICIT_INVOKE = UsageType(KotlinBundle.message("usageType.implicit.invoke"))
    val IMPLICIT_ITERATION = UsageType(KotlinBundle.message("usageType.implicit.iteration"))
    val PROPERTY_DELEGATION = UsageType(KotlinBundle.message("usageType.property.delegation"))

    // values
    val RECEIVER = UsageType(KotlinBundle.message("usageType.receiver"))
    val DELEGATE = UsageType(KotlinBundle.message("usageType.delegate"))

    // packages
    val PACKAGE_DIRECTIVE = UsageType(KotlinBundle.message("usageType.packageDirective"))
    val PACKAGE_MEMBER_ACCESS = UsageType(KotlinBundle.message("usageType.packageMemberAccess"))

    // common usage types
    val CALLABLE_REFERENCE = UsageType(KotlinBundle.message("usageType.callable.reference"))
    val NAMED_ARGUMENT = UsageType("Named argument")
}
