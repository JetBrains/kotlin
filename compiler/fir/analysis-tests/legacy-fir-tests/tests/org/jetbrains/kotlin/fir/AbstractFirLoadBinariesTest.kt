/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.fir.renderer.FirRenderer
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.test.KotlinTestUtils
import java.io.File

@OptIn(SymbolInternals::class)
abstract class AbstractFirLoadBinariesTest : AbstractFirResolveWithSessionTestCase() {
    /**
     * Since fir symbol providers can't get all names in package (only fir provider can do it),
     *   we should collect that names from module descriptor from FE 1.0
     */
    protected fun checkPackageContent(
        session: FirSession,
        packageFqName: FqName,
        moduleDescriptor: ModuleDescriptor,
        testDataPath: String
    ) {
        val declarationNames = DescriptorUtils.getAllDescriptors(moduleDescriptor.getPackage(packageFqName).memberScope)
            .mapTo(sortedSetOf()) { it.name }

        val provider = session.symbolProvider

        val builder = StringBuilder()
        val firRenderer = FirRenderer(builder)

        for (name in declarationNames) {
            for (symbol in provider.getTopLevelCallableSymbols(packageFqName, name)) {
                firRenderer.renderElementAsString(symbol.fir)
                builder.appendLine()
            }
        }

        for (name in declarationNames) {
            val classLikeSymbol = provider.getClassLikeSymbolByClassId(ClassId.topLevel(packageFqName.child(name))) ?: continue
            firRenderer.renderElementAsString(classLikeSymbol.fir)
            builder.appendLine()
        }

        KotlinTestUtils.assertEqualsToFile(
            File(testDataPath),
            builder.toString().trimEnd() + "\n"
        )
    }
}
