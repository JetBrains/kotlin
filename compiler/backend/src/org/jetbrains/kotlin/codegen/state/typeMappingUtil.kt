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

@file:JvmName("TypeMappingUtil")

package org.jetbrains.kotlin.codegen.state

import org.jetbrains.kotlin.builtins.StandardNames.FqNames
import org.jetbrains.kotlin.load.kotlin.TypeMappingMode
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.TypeSystemCommonBackendContext
import org.jetbrains.kotlin.types.model.KotlinTypeMarker
import org.jetbrains.kotlin.types.suppressWildcardsMode

val FqName?.isMethodWithDeclarationSiteWildcardsFqName: Boolean
    get() = this in METHODS_WITH_DECLARATION_SITE_WILDCARDS

private fun FqName.child(name: String): FqName = child(Name.identifier(name))
private val METHODS_WITH_DECLARATION_SITE_WILDCARDS = setOf(
    FqNames.mutableCollection.child("addAll"),
    FqNames.mutableList.child("addAll"),
    FqNames.mutableMap.child("putAll")
)

fun TypeSystemCommonBackendContext.extractTypeMappingModeFromAnnotation(
    callableSuppressWildcardsMode: Boolean?,
    outerType: KotlinTypeMarker,
    isForAnnotationParameter: Boolean,
    mapTypeAliases: Boolean
): TypeMappingMode? {
    val suppressWildcards =
        outerType.suppressWildcardsMode(this) ?: callableSuppressWildcardsMode ?: return null

    if (outerType.argumentsCount() == 0) return TypeMappingMode.DEFAULT

    return TypeMappingMode.createWithConstantDeclarationSiteWildcardsMode(
        skipDeclarationSiteWildcards = suppressWildcards,
        isForAnnotationParameter = isForAnnotationParameter,
        needInlineClassWrapping = !outerType.typeConstructor().isInlineClass(),
        mapTypeAliases = mapTypeAliases
    )
}
