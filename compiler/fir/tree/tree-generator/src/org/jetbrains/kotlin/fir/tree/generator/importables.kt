/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.tree.generator

import org.jetbrains.kotlin.fir.tree.generator.model.ArbitraryImportable

val phaseAsResolveStateExtentionImport = ArbitraryImportable("org.jetbrains.kotlin.fir.declarations", "asResolveState")
val resolvePhaseExtensionImport = ArbitraryImportable("org.jetbrains.kotlin.fir.declarations", "resolvePhase")
val resolveStateAccessImport = ArbitraryImportable("org.jetbrains.kotlin.fir.declarations", "ResolveStateAccess")
val resolvedDeclarationStatusImport = ArbitraryImportable("org.jetbrains.kotlin.fir.declarations.impl", "FirResolvedDeclarationStatusImpl")

val buildResolvedTypeRefImport = ArbitraryImportable("org.jetbrains.kotlin.fir.types.builder", "buildResolvedTypeRef")
val constructClassTypeImport = ArbitraryImportable("org.jetbrains.kotlin.fir.types", "constructClassType")
val toLookupTagImport = ArbitraryImportable("org.jetbrains.kotlin.fir.types", "toLookupTag")
