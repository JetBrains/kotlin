/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.fir.resolve.firSymbolProvider
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.test.ConfigurationKind
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.TestJdkKind
import java.io.File

class BuiltInsDeserializationForFirTestCase : AbstractFirResolveWithSessionTestCase() {
    override fun createEnvironment(): KotlinCoreEnvironment {
        return createEnvironmentWithJdk(ConfigurationKind.ALL, TestJdkKind.FULL_JDK)
    }

    fun testBuiltInPackagesContent() {
        for (packageFqName in listOf(
            KotlinBuiltIns.BUILT_INS_PACKAGE_FQ_NAME,
            KotlinBuiltIns.COLLECTIONS_PACKAGE_FQ_NAME,
            KotlinBuiltIns.RANGES_PACKAGE_FQ_NAME
        )) {
            checkPackageContent(packageFqName)
        }
    }

    private fun checkPackageContent(packageFqName: FqName) {
        val session = createSession(environment, GlobalSearchScope.allScope(project))
        val provider = session.firSymbolProvider

        val builder = StringBuilder()
        val firRenderer = FirRenderer(builder)

        for (name in provider.getAllCallableNamesInPackage(packageFqName)) {
            for (symbol in provider.getTopLevelCallableSymbols(packageFqName, name)) {
                symbol.fir.accept(firRenderer)
                builder.appendln()
            }
        }

        for (name in provider.getClassNamesInPackage(packageFqName)) {
            val classLikeSymbol =
                provider.getClassLikeSymbolByFqName(ClassId.topLevel(packageFqName.child(name))) as FirClassSymbol?
                    ?: continue
            classLikeSymbol.fir.accept(firRenderer)
            builder.appendln()
        }


        KotlinTestUtils.assertEqualsToFile(
            File("compiler/fir/analysis-tests/testData/builtIns/" + packageFqName.asString().replace('.', '-') + ".txt"),
            builder.toString()
        )
    }
}
