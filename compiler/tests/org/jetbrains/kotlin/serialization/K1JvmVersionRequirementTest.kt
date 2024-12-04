/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.serialization

import org.jetbrains.kotlin.config.JvmAnalysisFlags
import org.jetbrains.kotlin.config.JvmDefaultMode
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.deserialization.VersionRequirement

class K1JvmVersionRequirementTest : AbstractJvmVersionRequirementTest() {
    fun testAllJvmDefault() {
        doTest(
            VersionRequirement.Version(1, 4, 0), DeprecationLevel.ERROR, null,
            ProtoBuf.VersionRequirement.VersionKind.COMPILER_VERSION, null,
            analysisFlags = mapOf(JvmAnalysisFlags.jvmDefaultMode to JvmDefaultMode.ALL),
            fqNamesWithRequirements = listOf(
                "test.Base",
                "test.Derived",
                "test.BaseWithProperty",
                "test.DerivedWithProperty",
                "test.Empty",
                "test.EmptyWithNested",
                "test.WithAbstractDeclaration",
                "test.DerivedFromWithAbstractDeclaration"
            )
        )
    }

    fun testAllCompatibilityJvmDefault() {
        doTest(
            VersionRequirement.Version(1, 4, 0), DeprecationLevel.ERROR, null,
            ProtoBuf.VersionRequirement.VersionKind.COMPILER_VERSION, null,
            analysisFlags = mapOf(JvmAnalysisFlags.jvmDefaultMode to JvmDefaultMode.ALL_COMPATIBILITY),
            fqNamesWithRequirements = emptyList(),
            fqNamesWithoutRequirement = listOf(
                "test.Base",
                "test.Derived",
                "test.BaseWithProperty",
                "test.DerivedWithProperty",
                "test.Empty",
                "test.EmptyWithNested",
                "test.WithAbstractDeclaration",
                "test.DerivedFromWithAbstractDeclaration"
            )
        )
    }

    fun testInlineParameterNullCheck() {
        doTest(
            VersionRequirement.Version(1, 3, 50), DeprecationLevel.ERROR, null,
            ProtoBuf.VersionRequirement.VersionKind.COMPILER_VERSION, null,
            fqNamesWithRequirements = listOf(
                "test.doRun",
                "test.lambdaVarProperty",
                "test.extensionProperty"
            ),
            customLanguageVersion = LanguageVersion.KOTLIN_1_4
        )
    }

    fun testInlineClassReturnTypeMangled() {
        // Class members returning inline class values are mangled,
        // and have "language >= 1.4" and "compiler >= 1.4.30" version requirements.
        doTest(
            VersionRequirement.Version(1, 4, 0), DeprecationLevel.ERROR, null,
            ProtoBuf.VersionRequirement.VersionKind.LANGUAGE_VERSION, null,
            fqNamesWithRequirements = listOf(
                "test.C.returnsInlineClassType",
                "test.C.propertyOfInlineClassType"
            ),
            customLanguageVersion = LanguageVersion.KOTLIN_1_4,
            shouldBeSingleRequirement = false
        )
        doTest(
            VersionRequirement.Version(1, 4, 30), DeprecationLevel.ERROR, null,
            ProtoBuf.VersionRequirement.VersionKind.COMPILER_VERSION, null,
            fqNamesWithRequirements = listOf(
                "test.C.returnsInlineClassType",
                "test.C.propertyOfInlineClassType",
            ),
            customLanguageVersion = LanguageVersion.KOTLIN_1_4,
            shouldBeSingleRequirement = false,
        )
    }

    fun testInlineClassesAndRelevantDeclarations1430() {
        doTest(
            VersionRequirement.Version(1, 4, 30), DeprecationLevel.ERROR, null,
            ProtoBuf.VersionRequirement.VersionKind.COMPILER_VERSION, null,
            fqNamesWithRequirements = listOf(
                "test.simpleFun",
                "test.aliasedFun",
            ),
            shouldBeSingleRequirement = false
        )
    }
}
