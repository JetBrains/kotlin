/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.symbols.pointers

import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.descriptors.KaFe10Session
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.base.KaFe10EnumEntrySymbol
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.KaFe10DescEnumEntrySymbol
import org.jetbrains.kotlin.analysis.api.impl.base.symbols.pointers.KaBaseCachedSymbolPointer
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.findClassAcrossModuleDependencies
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name

internal class KaFe10DescEnumEntrySymbolPointer(
    private val classId: ClassId,
    private val entryName: Name,
    originalSymbol: KaFe10EnumEntrySymbol?,
) : KaBaseCachedSymbolPointer<KaFe10EnumEntrySymbol>(originalSymbol) {
    @KaImplementationDetail
    override fun restoreIfNotCached(analysisSession: KaSession): KaFe10EnumEntrySymbol? {
        check(analysisSession is KaFe10Session)
        val analysisContext = analysisSession.analysisContext

        val entryDescriptor = analysisContext.resolveSession.moduleDescriptor.findClassAcrossModuleDependencies(classId)
            ?.unsubstitutedMemberScope
            ?.getContributedClassifier(entryName, NoLookupLocation.FROM_IDE)

        if (entryDescriptor is ClassDescriptor && entryDescriptor.kind == ClassKind.ENUM_ENTRY) {
            return KaFe10DescEnumEntrySymbol(entryDescriptor, analysisContext)
        }

        return null
    }

    override fun pointsToTheSameSymbolAs(other: KaSymbolPointer<KaSymbol>): Boolean = this === other ||
            other is KaFe10DescEnumEntrySymbolPointer &&
            other.classId == classId &&
            other.entryName == entryName
}
