/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.symbols.pointers

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.descriptors.KaFe10Session
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.KaFe10DescNamedClassOrObjectSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaNamedClassOrObjectSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer
import org.jetbrains.kotlin.descriptors.findClassAcrossModuleDependencies
import org.jetbrains.kotlin.name.ClassId

class KaFe10DescNamedClassOrObjectSymbolSymbol(private val classId: ClassId) : KaSymbolPointer<KaNamedClassOrObjectSymbol>() {
    @Deprecated("Consider using org.jetbrains.kotlin.analysis.api.KaSession.restoreSymbol")
    override fun restoreSymbol(analysisSession: KaSession): KaNamedClassOrObjectSymbol? {
        check(analysisSession is KaFe10Session)
        val analysisContext = analysisSession.analysisContext

        val descriptor = analysisContext.resolveSession.moduleDescriptor.findClassAcrossModuleDependencies(classId) ?: return null
        return KaFe10DescNamedClassOrObjectSymbol(descriptor, analysisContext)
    }

    override fun pointsToTheSameSymbolAs(other: KaSymbolPointer<KaSymbol>): Boolean = this === other ||
            other is KaFe10DescNamedClassOrObjectSymbolSymbol &&
            other.classId == classId
}
