/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.symbols.pointers

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.descriptors.KaFe10Session
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.KaFe10DescEnumEntrySymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaEnumEntrySymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.findClassAcrossModuleDependencies
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name

class KaFe10DescEnumEntrySymbolPointer(private val classId: ClassId, private val entryName: Name) : KaSymbolPointer<KaEnumEntrySymbol>() {
    @Deprecated("Consider using org.jetbrains.kotlin.analysis.api.KaSession.restoreSymbol")
    override fun restoreSymbol(analysisSession: KaSession): KaEnumEntrySymbol? {
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
