/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.ObsoleteTestInfrastructure
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.serialization.builtins.BuiltinsTestUtils
import org.jetbrains.kotlin.test.ConfigurationKind
import org.jetbrains.kotlin.test.TestJdkKind

class BuiltInsDeserializationForFirTestCase : AbstractFirLoadBinariesTest() {
    override fun createEnvironment(): KotlinCoreEnvironment {
        return createEnvironmentWithJdk(ConfigurationKind.ALL, TestJdkKind.FULL_JDK)
    }

    @OptIn(ObsoleteTestInfrastructure::class)
    fun testBuiltInPackagesContent() {
        val moduleDescriptor = BuiltinsTestUtils.compileBuiltinsModule(environment)
        val session = createSessionForTests(environment, GlobalSearchScope.allScope(project))
        for (packageFqName in BuiltinsTestUtils.BUILTIN_PACKAGE_NAMES) {
            val path = "compiler/fir/analysis-tests/testData/builtIns/" + packageFqName.asString().replace('.', '-') + ".txt"
            checkPackageContent(session, packageFqName, moduleDescriptor, path)
        }
    }
}
