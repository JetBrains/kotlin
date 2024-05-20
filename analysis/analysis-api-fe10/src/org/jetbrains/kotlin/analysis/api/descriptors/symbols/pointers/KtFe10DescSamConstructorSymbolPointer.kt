/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.symbols.pointers

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.descriptors.KaFe10Session
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.KaFe10DescSamConstructorSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSamConstructorSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer
import org.jetbrains.kotlin.descriptors.findClassAcrossModuleDependencies
import org.jetbrains.kotlin.load.java.sam.JvmSamConversionOracle
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.resolve.sam.createSamConstructorFunction
import org.jetbrains.kotlin.resolve.sam.getSingleAbstractMethodOrNull

class KaFe10DescSamConstructorSymbolPointer(private val classId: ClassId) : KaSymbolPointer<KaSamConstructorSymbol>() {
    @Deprecated("Consider using org.jetbrains.kotlin.analysis.api.KaSession.restoreSymbol")
    override fun restoreSymbol(analysisSession: KaSession): KaSamConstructorSymbol? {
        check(analysisSession is KaFe10Session)
        val analysisContext = analysisSession.analysisContext

        val samInterface = analysisContext.resolveSession.moduleDescriptor.findClassAcrossModuleDependencies(classId)
        if (samInterface == null || getSingleAbstractMethodOrNull(samInterface) == null) {
            return null
        }

        val constructorDescriptor = createSamConstructorFunction(
            samInterface.containingDeclaration,
            samInterface,
            analysisContext.resolveSession.samConversionResolver,
            JvmSamConversionOracle(analysisContext.resolveSession.languageVersionSettings)
        )

        return KaFe10DescSamConstructorSymbol(constructorDescriptor, analysisContext)
    }

    override fun pointsToTheSameSymbolAs(other: KaSymbolPointer<KaSymbol>): Boolean = this === other ||
            other is KaFe10DescSamConstructorSymbolPointer &&
            other.classId == classId
}
