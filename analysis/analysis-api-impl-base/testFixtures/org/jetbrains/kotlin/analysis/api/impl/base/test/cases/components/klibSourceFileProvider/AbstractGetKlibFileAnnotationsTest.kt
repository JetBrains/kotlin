/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.klibSourceFileProvider

import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.components.klibFileAnnotationClassIds
import org.jetbrains.kotlin.analysis.api.projectStructure.KaLibraryModule
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtTestModule
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.ktTestModuleStructure
import org.jetbrains.kotlin.library.components.metadata
import org.jetbrains.kotlin.library.loader.KlibLoader
import org.jetbrains.kotlin.library.loader.reportLoadingProblemsIfAny
import org.jetbrains.kotlin.library.metadata.KlibMetadataProtoBuf
import org.jetbrains.kotlin.library.metadata.parseModuleHeader
import org.jetbrains.kotlin.library.metadata.parsePackageFragment
import org.jetbrains.kotlin.metadata.deserialization.NameResolverImpl
import org.jetbrains.kotlin.metadata.deserialization.getExtensionOrNull
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.serialization.deserialization.getName
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions
import kotlin.test.fail

/**
 * Reads through the declarations provided in the .klib and renders their `klibFileAnnotationClassIds`.
 */
abstract class AbstractGetKlibFileAnnotationsTest : AbstractAnalysisApiBasedTest() {
    override fun doTestByMainModuleAndOptionalMainFile(mainFile: KtFile?, mainModule: KtTestModule, testServices: TestServices) {
        val libraryModule = testServices.ktTestModuleStructure.mainModules
            .map { it.ktModule }
            .filterIsInstance<KaLibraryModule>()
            .singleOrNull()
            ?: fail("Expected a single library module to be present.")

        val actual = StringBuilder()
        actual.appendLine("klib declarations:")

        analyze(mainModule.ktModule) {
            val binaryRoot = libraryModule.binaryRoots.singleOrNull() ?: fail("Expected single binary root")

            val klibLoadingResult = KlibLoader { libraryPaths(binaryRoot) }.load()
            klibLoadingResult.reportLoadingProblemsIfAny { _, message -> fail(message) }

            val library = klibLoadingResult.librariesStdlibFirst.single()

            val metadata = library.metadata
            val headerProto = parseModuleHeader(metadata.moduleHeaderData)

            val packageMetadataSequence = headerProto.packageFragmentNameList.asSequence().flatMap { packageFragmentName ->
                metadata.getPackageFragmentNames(packageFragmentName).asSequence().map { packageMetadataPart ->
                    metadata.getPackageFragment(packageFragmentName, packageMetadataPart)
                }
            }

            packageMetadataSequence.forEach { packageMetadata ->
                val packageFragmentProto = parsePackageFragment(packageMetadata)
                val nameResolver = NameResolverImpl(packageFragmentProto.strings, packageFragmentProto.qualifiedNames)
                val packageFqName = packageFragmentProto.`package`.getExtensionOrNull(KlibMetadataProtoBuf.packageFqName)
                    ?.let { packageFqNameStringIndex -> nameResolver.getPackageFqName(packageFqNameStringIndex) }
                    ?.let { fqNameString -> FqName(fqNameString) }
                    ?: fail("Missing packageFqName")

                packageFragmentProto.class_List.forEach { classProto ->
                    val classId = ClassId.fromString(nameResolver.getQualifiedClassName(classProto.fqName))
                    val classSymbol = findClass(classId) ?: fail("Failed to find symbol '$classId'")
                    val fileAnnotations = classSymbol.klibFileAnnotationClassIds
                    actual.appendLine("Classifier: ${classSymbol.classId}; klibFileAnnotations: ${fileAnnotations?.map { it.asSingleFqName().asString() }}")
                }

                val propertyNames = packageFragmentProto.`package`.propertyList
                    .map { propertyProto -> nameResolver.getName(propertyProto.name) }

                val functionNames = packageFragmentProto.`package`.functionList
                    .map { functionProto -> nameResolver.getName(functionProto.name) }

                val callableNames = (propertyNames + functionNames).distinct()
                callableNames.forEach { callableName ->
                    findTopLevelCallables(packageFqName, callableName).forEach { symbol ->
                        val fileAnnotations = symbol.klibFileAnnotationClassIds
                        actual.appendLine("Callable: ${symbol.callableId}; klibFileAnnotations: ${fileAnnotations?.map { it.asSingleFqName().asString() }}")
                    }
                }
            }
        }

        testServices.assertions.assertEqualsToTestOutputFile(actual.toString())
    }
}
