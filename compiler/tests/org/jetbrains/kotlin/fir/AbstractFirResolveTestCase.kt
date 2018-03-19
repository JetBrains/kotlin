/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.descriptors.FirClassifierResolveTransformer
import org.jetbrains.kotlin.fir.builder.RawFirBuilder
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.resolve.*
import org.jetbrains.kotlin.fir.resolve.impl.FirProviderImpl
import org.jetbrains.kotlin.fir.resolve.impl.FirTypeResolverImpl
import org.jetbrains.kotlin.test.ConfigurationKind
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.KotlinTestWithEnvironment
import java.io.File

abstract class AbstractFirResolveTestCase : KotlinTestWithEnvironment() {
    override fun createEnvironment(): KotlinCoreEnvironment {
        return createEnvironmentWithMockJdk(ConfigurationKind.JDK_NO_RUNTIME)
    }

    fun doTest(path: String) {
        val file = File(path)
        val text = KotlinTestUtils.doLoadFile(file)

        val ktFile = KotlinTestUtils.createFile(file.name, text, project)


        val session = object : FirSessionBase() {
            init {
                registerComponent(FirProvider::class, FirProviderImpl(this))
                registerComponent(FirQualifierResolver::class, FirQualifierResolverImpl(this))
                registerComponent(FirTypeResolver::class, FirTypeResolverImpl())
            }
        }

        val builder = RawFirBuilder(session)
        val firFile = builder.buildFirFile(ktFile)

        (session.service<FirProvider>() as FirProviderImpl).recordFile(firFile)

        val transformer = FirClassifierResolveTransformer()
        firFile.transform<FirFile, Nothing?>(transformer, null)

        val firFileDump = StringBuilder().also { firFile.accept(FirRenderer(it), null) }.toString()
        val expectedPath = path.replace(".kt", ".txt")
        KotlinTestUtils.assertEqualsToFile(File(expectedPath), firFileDump)
    }
}