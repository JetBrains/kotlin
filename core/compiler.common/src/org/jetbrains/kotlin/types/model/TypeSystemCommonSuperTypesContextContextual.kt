/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.types.model

context(c: TypeSystemCommonSuperTypesContext)
fun KotlinTypeMarker.anySuperTypeConstructor(predicate: (RigidTypeMarker) -> Boolean) = with(c) { anySuperTypeConstructor(predicate) }

context(c: TypeSystemCommonSuperTypesContext)
fun KotlinTypeMarker.canHaveUndefinedNullability(): Boolean = with(c) { canHaveUndefinedNullability() }

context(c: TypeSystemCommonSuperTypesContext)
fun RigidTypeMarker.isExtensionFunction(): Boolean = with(c) { isExtensionFunction() }

context(c: TypeSystemCommonSuperTypesContext)
fun RigidTypeMarker.typeDepth(): Int = with(c) { typeDepth() }

context(c: TypeSystemCommonSuperTypesContext)
fun KotlinTypeMarker.typeDepth(): Int = with(c) { typeDepth() }

context(c: TypeSystemCommonSuperTypesContext)
fun KotlinTypeMarker.typeDepthForApproximation(): Int = with(c) { typeDepthForApproximation() }

context(c: TypeSystemCommonSuperTypesContext)
fun TypeConstructorMarker.toErrorType(): SimpleTypeMarker = with(c) { toErrorType() }

context(c: TypeSystemCommonSuperTypesContext)
fun KotlinTypeMarker.replaceCustomAttributes(newAttributes: List<AnnotationMarker>): KotlinTypeMarker =
    with(c) { replaceCustomAttributes(newAttributes) }