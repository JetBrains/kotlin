/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen

import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.cliArgument
import org.jetbrains.kotlin.cli.common.messages.MessageRenderer
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import org.jetbrains.kotlin.config.ApiVersion
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
import org.jetbrains.kotlin.load.kotlin.loadModuleMapping
import org.jetbrains.kotlin.metadata.jvm.deserialization.ModuleMapping
import org.jetbrains.kotlin.resolve.JvmCompilerDeserializationConfiguration
import org.jetbrains.kotlin.test.CompilerTestUtil
import org.jetbrains.kotlin.test.TestDataAssertions
import org.jetbrains.kotlin.test.testFramework.KtUsefulTestCase
import org.jetbrains.kotlin.test.util.KtTestUtil
import java.io.File

class JvmModuleProtoBufTest : KtUsefulTestCase() {
    private fun doTest(
        relativeDirectory: String,
        compileWith: LanguageVersion = LanguageVersion.LATEST_STABLE,
        loadWith: LanguageVersion = LanguageVersion.LATEST_STABLE,
        extraOptions: List<String> = emptyList(),
        messageRenderer: MessageRenderer? = null,
    ) {
        val directory = KtTestUtil.getTestDataPathBase() + relativeDirectory
        val tmpdir = KtTestUtil.tmpDir(this::class.simpleName)

        val moduleName = "main"
        CompilerTestUtil.executeCompilerAssertSuccessful(
            K2JVMCompiler(), listOf(
                directory,
                K2JVMCompilerArguments::destination.cliArgument, tmpdir.path,
                K2JVMCompilerArguments::moduleName.cliArgument, moduleName,
                CommonCompilerArguments::languageVersion.cliArgument, compileWith.versionString
            ) + extraOptions,
            messageRenderer
        )

        val mapping = ModuleMapping.loadModuleMapping(
            File(tmpdir, "META-INF/$moduleName.${ModuleMapping.MAPPING_FILE_EXT}").readBytes(), "test",
            JvmCompilerDeserializationConfiguration(LanguageVersionSettingsImpl(loadWith, ApiVersion.createByLanguageVersion(loadWith))),
            ::error
        )
        val result = buildString {
            for (annotationClassId in mapping.moduleData.annotations) {
                appendLine("@$annotationClassId")
            }
            for ((fqName, packageParts) in mapping.packageFqName2Parts) {
                appendLine(fqName)
                for (part in packageParts.parts) {
                    append("  ")
                    append(part)
                    val facadeName = packageParts.getMultifileFacadeName(part)
                    if (facadeName != null) {
                        append(" (")
                        append(facadeName)
                        append(")")
                    }
                    appendLine()
                }
            }
        }

        TestDataAssertions.assertEqualsToFile(File(directory, "module-proto.txt"), result)
    }

    fun testSimple() {
        doTest("/moduleProtoBuf/simple")
    }

    fun testJvmPackageName() {
        doTest("/moduleProtoBuf/jvmPackageName")
    }

    fun testJvmPackageNameManyParts() {
        doTest("/moduleProtoBuf/jvmPackageNameManyParts")
    }

    fun testJvmPackageNameLanguageVersion11() {
        doTest("/moduleProtoBuf/jvmPackageNameLanguageVersion11", loadWith = LanguageVersion.KOTLIN_1_1)
    }

    fun testJvmPackageNameMultifileClass() {
        doTest("/moduleProtoBuf/jvmPackageNameMultifileClass")
    }
}
