/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir

import org.jetbrains.kotlin.analysis.low.level.api.fir.backReferences.backReferencedFirFile
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirFile

// TODO (marco): Document.
// TODO (marco): Turn this into a registry flag (cheap to check since it controls the symbol ID factory, not read during symbol ID
//  creation).
internal const val ENABLE_SOURCE_BASED_SYMBOL_IDS = true

/**
 * A feature flag for the "Back references to FIR" prototype (KT-70517).
 *
 * When enabled, every [FirDeclaration] built from a Kotlin source [FirFile] in the Analysis API receives a
 * [back reference][backReferencedFirFile] to its containing [FirFile]. The intent is to allow FIR symbols, declarations, and files to be
 * *weakly* referenced from caches (symbol providers, the file cache, etc.) while still guaranteeing symbol uniqueness: as long as any
 * declaration of a file is used (kept alive via its symbol or a `Ka*` entity), the back reference keeps the whole file tree alive, so the
 * file cannot be rebuilt and produce duplicate FIR.
 *
 * This is the *assignment* half of the prototype. It is deliberately additive and behavior-preserving: it only attaches attributes and does
 * not yet weaken any cache references.
 */
internal const val ENABLE_FIR_FILE_BACK_REFERENCES: Boolean = true
