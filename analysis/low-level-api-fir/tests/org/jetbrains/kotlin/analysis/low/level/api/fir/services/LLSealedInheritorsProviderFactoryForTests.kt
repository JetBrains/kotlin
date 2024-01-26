/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.services

import org.jetbrains.kotlin.analysis.low.level.api.fir.api.services.LLSealedInheritorsProviderFactory
import org.jetbrains.kotlin.analysis.low.level.api.fir.project.structure.llFirModuleData
import org.jetbrains.kotlin.analysis.project.structure.KtDanglingFileModule
import org.jetbrains.kotlin.analysis.project.structure.KtModule
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.SealedClassInheritorsProvider
import org.jetbrains.kotlin.fir.declarations.SealedClassInheritorsProviderInternals
import org.jetbrains.kotlin.fir.declarations.sealedInheritorsAttr
import org.jetbrains.kotlin.fir.declarations.utils.classId
import org.jetbrains.kotlin.name.ClassId

internal class LLSealedInheritorsProviderFactoryForTests : LLSealedInheritorsProviderFactory {
    private val inheritorsByModule = mutableMapOf<KtModule, Map<ClassId, List<ClassId>>>()

    fun registerInheritors(ktModule: KtModule, inheritors: Map<ClassId, List<ClassId>>) {
        inheritorsByModule[ktModule] = inheritors
    }

    override fun createSealedInheritorsProvider(): SealedClassInheritorsProvider {
        return SealedClassInheritorsProviderForTests(inheritorsByModule)
    }
}

private class SealedClassInheritorsProviderForTests(
    private val inheritorsByModule: Map<KtModule, Map<ClassId, List<ClassId>>>
) : SealedClassInheritorsProvider() {
    @OptIn(SealedClassInheritorsProviderInternals::class)
    override fun getSealedClassInheritors(firClass: FirRegularClass): List<ClassId> {
        val ktModule = firClass.llFirModuleData.ktModule
        val relevantModule = when (ktModule) {
            is KtDanglingFileModule -> ktModule.contextModule
            else -> ktModule
        }

        // the module might be absent in the map if it doesn't have any .kt-files whatsoever, in which case
        // there's definitely no sealed inheritors
        val inheritorsForModuleMap = inheritorsByModule[relevantModule] ?: return emptyList()
        return inheritorsForModuleMap[firClass.classId] ?: firClass.sealedInheritorsAttr?.value ?: emptyList()
    }
}
