/*
 * Copyright 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.plugins.kotlin

import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.container.StorageComponentContainer
import org.jetbrains.kotlin.container.useInstance
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.extensions.StorageComponentContainerContributor
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtExpression
import androidx.compose.plugins.kotlin.analysis.ComposeErrors
import org.jetbrains.kotlin.resolve.TargetPlatform
import org.jetbrains.kotlin.resolve.calls.checkers.AdditionalTypeChecker
import org.jetbrains.kotlin.resolve.calls.context.ResolutionContext
import org.jetbrains.kotlin.resolve.constants.ArrayValue
import org.jetbrains.kotlin.resolve.jvm.platform.JvmPlatform
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.checker.KotlinTypeChecker

class UnionAnnotationCheckerProvider() : StorageComponentContainerContributor {
    override fun registerModuleComponents(
        container: StorageComponentContainer,
        platform: TargetPlatform,
        moduleDescriptor: ModuleDescriptor
    ) {
        if (platform != JvmPlatform) return
        container.useInstance(
            UnionAnnotationChecker(
                moduleDescriptor
            )
        )
    }
}

class UnionAnnotationChecker(val moduleDescriptor: ModuleDescriptor) : AdditionalTypeChecker {
    companion object {
        val UNIONTYPE_ANNOTATION_NAME =
            ComposeUtils.composeFqName("UnionType")
    }

    override fun checkType(
        expression: KtExpression,
        expressionType: KotlinType,
        expressionTypeWithSmartCast: KotlinType,
        c: ResolutionContext<*>
    ) {
        val expectedType = c.expectedType
        if (TypeUtils.noExpectedType(expectedType)) return

        if (!expectedType.annotations.hasAnnotation(UNIONTYPE_ANNOTATION_NAME) &&
            !expressionTypeWithSmartCast.annotations.hasAnnotation(UNIONTYPE_ANNOTATION_NAME)) {
            return
        }

        val expressionTypes = getUnionTypes(expressionTypeWithSmartCast)
        val permittedTypes = getUnionTypes(expectedType)

        outer@ for (potentialExpressionType in expressionTypes) {
            for (permittedType in permittedTypes) {
                if (KotlinTypeChecker.DEFAULT.isSubtypeOf(potentialExpressionType, permittedType))
                    continue@outer
            }
            c.trace.report(
                ComposeErrors.ILLEGAL_ASSIGN_TO_UNIONTYPE.on(
                    expression,
                    listOf(potentialExpressionType),
                    permittedTypes
                )
            )
            return
        }
    }

    private fun getUnionTypes(type: KotlinType): List<KotlinType> {
        val annotation =
            type.annotations.findAnnotation(UNIONTYPE_ANNOTATION_NAME) ?: return listOf(type)
        val types = annotation.allValueArguments.get(Name.identifier("types")) as ArrayValue
        return types.value.map { it.getType(moduleDescriptor).arguments.single().type }
    }
}
