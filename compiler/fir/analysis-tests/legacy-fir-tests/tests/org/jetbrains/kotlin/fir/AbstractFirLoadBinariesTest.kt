/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.fir.resolve.firSymbolProvider
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import org.jetbrains.kotlin.test.KotlinTestUtils
import java.io.File

abstract class AbstractFirLoadBinariesTest : AbstractFirResolveWithSessionTestCase() {
    /**
     * Since fir symbol providers can't get all names in package (only fir provider can do it),
     *   we should collect that names from module descriptor from FE 1.0
     */
    protected fun checkPackageContent(
        session: FirSession,
        packageFqName: FqName,
        moduleDescriptor: ModuleDescriptor,
        testDataPath: String,
        originalTestDataPath: String?
    ) {
        val declarationNames = DescriptorUtils.getAllDescriptors(moduleDescriptor.getPackage(packageFqName).memberScope)
            .mapTo(sortedSetOf()) { it.name }

        val provider = session.firSymbolProvider

        val builder = StringBuilder()
        val firRenderer = FirRenderer(builder)

        for (name in declarationNames) {
            for (symbol in provider.getTopLevelCallableSymbols(packageFqName, name)) {
                symbol.fir.accept(firRenderer)
                builder.appendLine()
            }
        }

        for (name in declarationNames) {
            val classLikeSymbol = provider.getClassLikeSymbolByFqName(ClassId.topLevel(packageFqName.child(name))) ?: continue
            classLikeSymbol.fir.accept(firRenderer)
            builder.appendLine()
        }

        val isIgnored = originalTestDataPath?.let {
            InTextDirectivesUtils.isDirectiveDefined(File(originalTestDataPath).readText(), "// IGNORE_FIR")
        } ?: false

        var wasError = false
        try {
            KotlinTestUtils.assertEqualsToFile(
                File(testDataPath),
                builder.toString()
            )
        } catch (e: Throwable) {
            if (!isIgnored) throw e
            wasError = true
        }
        if (isIgnored && !wasError) {
            throw AssertionError("Looks like test can be unmuted. Please remove `// IGNORE_FIR` directive")
        }
    }
}
