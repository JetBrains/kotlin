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
import org.jetbrains.kotlin.idea.JetBundle
import org.jetbrains.kotlin.idea.findUsages.UsageTypeEnum.*

public object JetUsageTypeProvider : UsageTypeProviderEx {
    public override fun getUsageType(element: PsiElement?): UsageType? {
        return getUsageType(element, UsageTarget.EMPTY_ARRAY)
    }

    public override fun getUsageType(element: PsiElement?, targets: Array<out UsageTarget>): UsageType? {
        val usageType = UsageTypeUtils.getUsageType(element)
        if (usageType == null) return null
        return convertEnumToUsageType(usageType)
    }

    fun convertEnumToUsageType(usageType: UsageTypeEnum): UsageType {
        return when (usageType) {
            TYPE_CONSTRAINT -> JetUsageTypes.TYPE_CONSTRAINT
            VALUE_PARAMETER_TYPE -> JetUsageTypes.VALUE_PARAMETER_TYPE
            NON_LOCAL_PROPERTY_TYPE -> JetUsageTypes.NON_LOCAL_PROPERTY_TYPE
            FUNCTION_RETURN_TYPE -> JetUsageTypes.FUNCTION_RETURN_TYPE
            SUPER_TYPE -> JetUsageTypes.SUPER_TYPE
            TYPE_DEFINITION -> JetUsageTypes.TYPE_DEFINITION
            IS -> JetUsageTypes.IS
            CLASS_OBJECT_ACCESS -> JetUsageTypes.CLASS_OBJECT_ACCESS
            DEFAULT_OBJECT_ACCESS -> JetUsageTypes.DEFAULT_OBJECT_ACCESS
            EXTENSION_RECEIVER_TYPE -> JetUsageTypes.EXTENSION_RECEIVER_TYPE
            SUPER_TYPE_QUALIFIER -> JetUsageTypes.SUPER_TYPE_QUALIFIER

            FUNCTION_CALL -> JetUsageTypes.FUNCTION_CALL
            IMPLICIT_GET -> JetUsageTypes.IMPLICIT_GET
            IMPLICIT_SET -> JetUsageTypes.IMPLICIT_SET
            IMPLICIT_INVOKE -> JetUsageTypes.IMPLICIT_INVOKE
            IMPLICIT_ITERATION -> JetUsageTypes.IMPLICIT_ITERATION
            PROPERTY_DELEGATION -> JetUsageTypes.PROPERTY_DELEGATION

            RECEIVER -> JetUsageTypes.RECEIVER
            DELEGATE -> JetUsageTypes.DELEGATE

            PACKAGE_DIRECTIVE -> JetUsageTypes.PACKAGE_DIRECTIVE
            PACKAGE_MEMBER_ACCESS -> JetUsageTypes.PACKAGE_MEMBER_ACCESS

            CALLABLE_REFERENCE -> JetUsageTypes.CALLABLE_REFERENCE

            READ -> UsageType.READ
            WRITE -> UsageType.WRITE
            CLASS_IMPORT -> UsageType.CLASS_IMPORT
            CLASS_LOCAL_VAR_DECLARATION -> UsageType.CLASS_LOCAL_VAR_DECLARATION
            TYPE_PARAMETER -> UsageType.TYPE_PARAMETER
            CLASS_CAST_TO -> UsageType.CLASS_CAST_TO
            ANNOTATION -> UsageType.ANNOTATION
            CLASS_NEW_OPERATOR -> UsageType.CLASS_NEW_OPERATOR
        }
    }
}

object JetUsageTypes {
    // types
    val TYPE_CONSTRAINT = UsageType(JetBundle.message("usageType.type.constraint"))
    val VALUE_PARAMETER_TYPE = UsageType(JetBundle.message("usageType.value.parameter.type"))
    val NON_LOCAL_PROPERTY_TYPE = UsageType(JetBundle.message("usageType.nonLocal.property.type"))
    val FUNCTION_RETURN_TYPE = UsageType(JetBundle.message("usageType.function.return.type"))
    val SUPER_TYPE = UsageType(JetBundle.message("usageType.superType"))
    val TYPE_DEFINITION = UsageType(JetBundle.message("usageType.type.definition"))
    val IS = UsageType(JetBundle.message("usageType.is"))
    val CLASS_OBJECT_ACCESS = UsageType(JetBundle.message("usageType.class.object"))
    val DEFAULT_OBJECT_ACCESS = UsageType(JetBundle.message("usageType.default.object"))
    val EXTENSION_RECEIVER_TYPE = UsageType(JetBundle.message("usageType.extension.receiver.type"))
    val SUPER_TYPE_QUALIFIER = UsageType(JetBundle.message("usageType.super.type.qualifier"))

    // functions
    val FUNCTION_CALL = UsageType(JetBundle.message("usageType.function.call"))
    val IMPLICIT_GET = UsageType(JetBundle.message("usageType.implicit.get"))
    val IMPLICIT_SET = UsageType(JetBundle.message("usageType.implicit.set"))
    val IMPLICIT_INVOKE = UsageType(JetBundle.message("usageType.implicit.invoke"))
    val IMPLICIT_ITERATION = UsageType(JetBundle.message("usageType.implicit.iteration"))
    val PROPERTY_DELEGATION = UsageType(JetBundle.message("usageType.property.delegation"))

    // values
    val RECEIVER = UsageType(JetBundle.message("usageType.receiver"))
    val DELEGATE = UsageType(JetBundle.message("usageType.delegate"))

    // packages
    val PACKAGE_DIRECTIVE = UsageType(JetBundle.message("usageType.packageDirective"))
    val PACKAGE_MEMBER_ACCESS = UsageType(JetBundle.message("usageType.packageMemberAccess"))

    // common usage types
    val CALLABLE_REFERENCE = UsageType(JetBundle.message("usageType.callable.reference"))
}
