/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.library.abi

import org.jetbrains.kotlin.cli.common.arguments.K2JSCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.cliArgument
import org.jetbrains.kotlin.cli.js.K2JSCompiler
import org.jetbrains.kotlin.library.KotlinIrSignatureVersion
import org.jetbrains.kotlin.test.CompilerTestUtil
import org.jetbrains.kotlin.test.services.configuration.JsEnvironmentConfigurator
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

/**
 * Checks that [AbiDeclarationOrigin.VERSION_OVERLOAD_WRAPPER] is reported exactly for the functions and constructors
 * that `VersionOverloadsLowering` synthesizes for `@IntroducedAt`-annotated parameters, and that everything else in
 * the library is reported as [AbiDeclarationOrigin.OTHER].
 *
 * Origins are read from the IR declaration origin serialized in the KLIB, and they are not rendered into the textual
 * ABI dump. So, unlike the other properties of [AbiFunction], they can't be observed through the golden dumps in
 * `compiler/testData/klib/dump-abi`, and need a real KLIB compiled from sources to be tested.
 *
 * Note that hand-written `@Deprecated` declarations must be reported as [AbiDeclarationOrigin.OTHER]: the synthesized
 * wrappers are annotated with `@Deprecated(level = ERROR)` too, so the check has to rely on the declaration origin
 * alone. The only consumer of the origins, `KlibAbiDumpAfterInliningVerifyingHandler`, would stop verifying
 * hand-written declarations otherwise.
 *
 * [AbiDeclarationOrigin.SYNTHETIC_ACCESSOR] is deliberately not covered here. Getting such an accessor into the ABI
 * needs an inline function that is actually inlined across a module boundary, which a single-module CLI compile like
 * the one below does not produce; leaking a private declaration out of a single module is a compile error instead.
 * The `AbstractJsKlibSyntheticAccessorsBoxTest` suites over `compiler/testData/klib/syntheticAccessors` cover it:
 * were accessors reported as [AbiDeclarationOrigin.OTHER] there, they would no longer be excluded from the
 * after-inlining ABI comparison and those tests would fail.
 */
@OptIn(ExperimentalLibraryAbiReader::class)
class VersionOverloadWrappersReadingTest {
    @TempDir
    private lateinit var buildDir: File

    @Test
    fun testVersionOverloadWrappersAreMarked() {
        val libraryAbi = LibraryAbiReader.readAbiInfo(compileJsKlib())

        assertEquals(EXPECTED_DECLARATION_ORIGINS, libraryAbi.renderDeclarationOrigins())
    }

    /**
     * Compiles [LIBRARY_SOURCES] into a KLIB with the JS compiler, which runs `VersionOverloadsLowering`
     * before serialization.
     */
    private fun compileJsKlib(): File {
        val sourceFile = buildDir.resolve("$LIBRARY_NAME.kt").apply { writeText(LIBRARY_SOURCES) }
        val outputDir = buildDir.resolve("out")

        CompilerTestUtil.executeCompilerAssertSuccessful(
            K2JSCompiler(),
            listOf(
                sourceFile.absolutePath,
                K2JSCompilerArguments::libraries.cliArgument(JsEnvironmentConfigurator.stdlibPath),
                K2JSCompilerArguments::outputDir.cliArgument(outputDir.absolutePath),
                K2JSCompilerArguments::moduleName.cliArgument(LIBRARY_NAME),
            )
        )

        return outputDir.resolve("$LIBRARY_NAME.klib")
    }

    /**
     * Renders `<origin> <signature>` for every [AbiFunction] in the library, sorted by signature, so that a wrapper
     * always ends up next to the declaration it wraps.
     */
    private fun LibraryAbi.renderDeclarationOrigins(): String {
        return topLevelDeclarations.allFunctions()
            .sortedBy { function -> function.signatures[SIGNATURE_VERSION] }
            .joinToString("\n") { function ->
                // `padEnd` only ever pads, so a longer origin name added later cannot silently shift these lines.
                "${function.declarationOrigin.name.padEnd(ORIGIN_COLUMN_WIDTH)} ${function.signatures[SIGNATURE_VERSION]}"
            }
    }

    companion object {
        private const val LIBRARY_NAME = "versionOverloadWrappers"
        private const val ORIGIN_COLUMN_WIDTH = 24

        private val SIGNATURE_VERSION = AbiSignatureVersion.resolveByVersionNumber(KotlinIrSignatureVersion.V2.number)

        private val LIBRARY_SOURCES = """
            @file:OptIn(ExperimentalVersionOverloading::class)

            fun topLevelFun(a: Int, @IntroducedAt("1") b: String = "", @IntroducedAt("2") c: Boolean = false) = "${'$'}a${'$'}b${'$'}c"

            // A hand-written declaration that is annotated exactly like a synthesized wrapper. Must be OTHER.
            @Deprecated("Not a version overload wrapper", level = DeprecationLevel.ERROR)
            fun deprecatedByHand(a: Int = 0) = a

            // Both at once: the wrapper generated for this one ends up with two `@Deprecated` annotations, since
            // `VersionOverloadsLowering` copies the hand-written one over and then adds its own. Only the wrapper
            // is a VERSION_OVERLOAD_WRAPPER.
            @Deprecated("Deprecated and versioned", level = DeprecationLevel.ERROR)
            fun versionedAndDeprecatedByHand(a: Int, @IntroducedAt("1") b: String = "") = "${'$'}a${'$'}b"

            class WithVersionedConstructor(a: Int, @IntroducedAt("1") b: String = "") {
                fun memberFun(a: Int, @IntroducedAt("1") b: String = "") = "${'$'}a${'$'}b"
            }

            data class VersionedDataClass(val a: Int, @IntroducedAt("1") val b: String = "")
        """.trimIndent()

        private val EXPECTED_DECLARATION_ORIGINS = """
            VERSION_OVERLOAD_WRAPPER /VersionedDataClass.<init>|<init>(kotlin.Int){}[0]
            OTHER                    /VersionedDataClass.<init>|<init>(kotlin.Int;kotlin.String){}[0]
            OTHER                    /VersionedDataClass.a.<get-a>|<get-a>(){}[0]
            OTHER                    /VersionedDataClass.b.<get-b>|<get-b>(){}[0]
            OTHER                    /VersionedDataClass.component1|component1(){}[0]
            OTHER                    /VersionedDataClass.component2|component2(){}[0]
            VERSION_OVERLOAD_WRAPPER /VersionedDataClass.copy|copy(kotlin.Int){}[0]
            OTHER                    /VersionedDataClass.copy|copy(kotlin.Int;kotlin.String){}[0]
            OTHER                    /VersionedDataClass.equals|equals(kotlin.Any?){}[0]
            OTHER                    /VersionedDataClass.hashCode|hashCode(){}[0]
            OTHER                    /VersionedDataClass.toString|toString(){}[0]
            VERSION_OVERLOAD_WRAPPER /WithVersionedConstructor.<init>|<init>(kotlin.Int){}[0]
            OTHER                    /WithVersionedConstructor.<init>|<init>(kotlin.Int;kotlin.String){}[0]
            VERSION_OVERLOAD_WRAPPER /WithVersionedConstructor.memberFun|memberFun(kotlin.Int){}[0]
            OTHER                    /WithVersionedConstructor.memberFun|memberFun(kotlin.Int;kotlin.String){}[0]
            OTHER                    /deprecatedByHand|deprecatedByHand(kotlin.Int){}[0]
            VERSION_OVERLOAD_WRAPPER /topLevelFun|topLevelFun(kotlin.Int){}[0]
            VERSION_OVERLOAD_WRAPPER /topLevelFun|topLevelFun(kotlin.Int;kotlin.String){}[0]
            OTHER                    /topLevelFun|topLevelFun(kotlin.Int;kotlin.String;kotlin.Boolean){}[0]
            VERSION_OVERLOAD_WRAPPER /versionedAndDeprecatedByHand|versionedAndDeprecatedByHand(kotlin.Int){}[0]
            OTHER                    /versionedAndDeprecatedByHand|versionedAndDeprecatedByHand(kotlin.Int;kotlin.String){}[0]
        """.trimIndent()
    }
}
