/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.tree.generator

import org.jetbrains.kotlin.generators.tree.ArbitraryImportable

val phaseAsResolveStateExtentionImport = ArbitraryImportable("org.jetbrains.kotlin.fir.declarations", "asResolveState")
val resolvePhaseExtensionImport = ArbitraryImportable("org.jetbrains.kotlin.fir.declarations", "resolvePhase")
val resolvedDeclarationStatusImport = ArbitraryImportable("org.jetbrains.kotlin.fir.declarations.impl", "FirResolvedDeclarationStatusImpl")

val constructClassTypeImport = ArbitraryImportable("org.jetbrains.kotlin.fir.types", "constructClassType")
val constructClassLikeTypeImport = ArbitraryImportable("org.jetbrains.kotlin.fir.types", "constructClassLikeType")
val toLookupTagImport = ArbitraryImportable("org.jetbrains.kotlin.fir.types", "toLookupTag")
val coneTypeOrNullImport = ArbitraryImportable("org.jetbrains.kotlin.fir.types", "coneTypeOrNull")

val fakeSourceElementKindImport = ArbitraryImportable("org.jetbrains.kotlin", "KtFakeSourceElementKind")
val fakeElementImport = ArbitraryImportable("org.jetbrains.kotlin", "fakeElement")

val transformInPlaceImport = ArbitraryImportable(VISITOR_PACKAGE, "transformInplace")
val toMutableOrEmptyImport = ArbitraryImportable("org.jetbrains.kotlin.fir.builder", "toMutableOrEmpty")
