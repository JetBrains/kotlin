/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.library.abi

import org.jetbrains.kotlin.library.abi.impl.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

@OptIn(ExperimentalLibraryAbiReader::class)
class AbiTypeArgumentRenderingTest {
    @ParameterizedTest
    @EnumSource(AbiSignatureVersions.Supported::class)
    internal fun test(signatureVersion: AbiSignatureVersions.Supported) {
        val mockLibraryAbi = mockLibraryAbi(
            signatureVersion,
            mockClass(
                name = "FinalClass",
                mockType(
                    "sample", "OpenClass",
                    mockType("sample", "InvariantClass") to AbiVariance.INVARIANT,
                    mockType("sample", "InClass") to AbiVariance.IN,
                    mockType("sample", "OutClass") to AbiVariance.OUT,
                    null
                )
            )
        )

        val renderedClass = LibraryAbiRenderer.render(mockLibraryAbi, AbiRenderingSettings(signatureVersion))
            .lineSequence()
            .filter(String::isNotBlank)
            .last()

        assertEquals(
            "final class sample/FinalClass : sample/OpenClass<sample/InvariantClass, in sample/InClass, out sample/OutClass, *> // signature-v${signatureVersion.versionNumber}",
            renderedClass
        )
    }

    private fun mockLibraryAbi(signatureVersion: AbiSignatureVersion, vararg declarations: AbiDeclaration): LibraryAbi =
        LibraryAbi(
            manifest = LibraryManifest(null, emptyList(), null, null, null, null),
            uniqueName = "type-argument-rendering-test",
            signatureVersions = setOf(signatureVersion),
            topLevelDeclarations = object : AbiTopLevelDeclarations {
                override val declarations = declarations.toList()
            }
        )

    private fun mockClass(name: String, vararg superTypes: AbiType): AbiClass =
        AbiClassImpl(
            qualifiedName = AbiQualifiedName(AbiCompoundName("sample"), AbiCompoundName(name)),
            signatures = AbiSignaturesImpl("signature-v1", "signature-v2"),
            annotations = emptySet(),
            modality = AbiModality.FINAL,
            kind = AbiClassKind.CLASS,
            isInner = false,
            isValue = false,
            isFunction = false,
            superTypes = superTypes.toList(),
            declarations = emptyList(),
            typeParameters = emptyList()
        )

    private fun mockType(packageName: String, className: String, vararg arguments: Pair<AbiType, AbiVariance>?): AbiType {
        return SimpleTypeImpl(
            classifierReference = ClassReferenceImpl(AbiQualifiedName(AbiCompoundName(packageName), AbiCompoundName(className))),
            arguments = arguments.map { argument ->
                if (argument == null)
                    StarProjectionImpl
                else
                    TypeProjectionImpl(argument.first, argument.second)
            },
            nullability = AbiTypeNullability.NOT_SPECIFIED
        )
    }
}