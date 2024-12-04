/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.java.enhancement

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.resolve.transformers.plugin.CompilerRequiredAnnotationEnhancementProvider
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirEnumEntrySymbol
import org.jetbrains.kotlin.name.JvmStandardClassIds

object JavaCompilerRequiredAnnotationEnhancementProvider : CompilerRequiredAnnotationEnhancementProvider() {
    override fun enhance(enumSymbol: FirClassSymbol<*>, enumEntrySymbol: FirEnumEntrySymbol, session: FirSession): FirEnumEntrySymbol {
        val firRegularClass = enumSymbol.fir as? FirRegularClass ?: return enumEntrySymbol
        if (enumSymbol.classId != JvmStandardClassIds.Annotations.Java.ElementType) return enumEntrySymbol

        return FirSignatureEnhancement(firRegularClass, session) { emptyList() }.enhancedProperty(
            enumEntrySymbol,
            enumEntrySymbol.name,
        ) as FirEnumEntrySymbol
    }
}
