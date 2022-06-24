/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.symbols

import org.jetbrains.kotlin.analysis.api.KtAnalysisApiInternals
import org.jetbrains.kotlin.analysis.api.symbols.KtClassKind
import org.jetbrains.kotlin.descriptors.ClassKind

@KtAnalysisApiInternals
fun ClassKind.toKtClassKind(isCompanionObject: Boolean): KtClassKind = when (this) {
    ClassKind.INTERFACE -> KtClassKind.INTERFACE
    ClassKind.ENUM_CLASS -> KtClassKind.ENUM_CLASS
    ClassKind.ANNOTATION_CLASS -> KtClassKind.ANNOTATION_CLASS
    ClassKind.CLASS -> KtClassKind.CLASS
    ClassKind.OBJECT -> if (isCompanionObject) KtClassKind.COMPANION_OBJECT else KtClassKind.OBJECT
    ClassKind.ENUM_ENTRY -> invalidEnumEntryAsClassKind()
}

@KtAnalysisApiInternals
fun invalidEnumEntryAsClassKind(): Nothing {
    error("KtClassKind is not applicable for enum entry, as enum entry is a callable, not a classifier")
}