/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.fir.resolve.FirSymbolProvider
import org.jetbrains.kotlin.fir.symbols.CallableId
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.test.ConfigurationKind
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.TestJdkKind
import java.io.File

class BuiltInsDeserializationForFirTestCase : AbstractFirResolveWithSessionTestCase() {
    override fun createEnvironment(): KotlinCoreEnvironment {
        return createEnvironmentWithJdk(ConfigurationKind.ALL, TestJdkKind.FULL_JDK)
    }

    fun testCallables() {
        for (packageFqName in listOf(
            KotlinBuiltIns.BUILT_INS_PACKAGE_FQ_NAME,
            KotlinBuiltIns.COLLECTIONS_PACKAGE_FQ_NAME,
            KotlinBuiltIns.RANGES_PACKAGE_FQ_NAME
        )) {
            checkCallablesInPackage(packageFqName)
        }
    }

    private fun checkCallablesInPackage(packageFqName: FqName) {
        val session = createSession(project, GlobalSearchScope.allScope(project))
        val provider = session.getService(FirSymbolProvider::class)
        val names = provider.getAllCallableNamesInPackage(packageFqName)

        val builder = StringBuilder()
        val firRenderer = FirRenderer(builder)

        for (name in names) {
            for (symbol in provider.getCallableSymbols(CallableId(packageFqName, null, name))) {
                (symbol as FirCallableSymbol).fir.accept(firRenderer)
                builder.appendln()
            }
        }

        KotlinTestUtils.assertEqualsToFile(
            File("compiler/fir/resolve/testData/builtIns/" + packageFqName.asString().replace('.', '-') + ".txt"),
            builder.toString()
        )
    }
}
