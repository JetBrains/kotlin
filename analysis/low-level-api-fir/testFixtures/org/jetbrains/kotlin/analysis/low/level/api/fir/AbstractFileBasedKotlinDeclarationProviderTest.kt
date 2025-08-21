/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir

import org.jetbrains.kotlin.analysis.api.impl.base.util.callableId
import org.jetbrains.kotlin.analysis.api.platform.declarations.KotlinFileBasedDeclarationProvider
import org.jetbrains.kotlin.analysis.low.level.api.fir.test.configurators.AnalysisApiFirScriptTestConfigurator
import org.jetbrains.kotlin.analysis.low.level.api.fir.test.configurators.AnalysisApiFirSourceTestConfigurator
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtTestModule
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.directives.model.SimpleDirectivesContainer
import org.jetbrains.kotlin.test.services.TestModuleStructure
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.moduleStructure
import kotlin.test.assertContains
import kotlin.test.assertNotNull

abstract class AbstractFileBasedKotlinDeclarationProviderTest : AbstractAnalysisApiBasedTest() {
    override val additionalDirectives: List<DirectivesContainer>
        get() = super.additionalDirectives + listOf(Directives)

    override fun doTestByMainFile(mainFile: KtFile, mainModule: KtTestModule, testServices: TestServices) {
        val provider = KotlinFileBasedDeclarationProvider(mainFile)
        assertContains(provider.findFilesForFacadeByPackage(mainFile.packageFqName), mainFile)

        checkByDirectives(testServices.moduleStructure, provider)
        checkByVisitor(mainFile, provider)
    }

    private fun checkByDirectives(moduleStructure: TestModuleStructure, provider: KotlinFileBasedDeclarationProvider) {
        for (directive in moduleStructure.allDirectives[Directives.CLASS]) {
            val classId = ClassId.fromString(directive)
            assert(provider.getAllClassesByClassId(classId).isNotEmpty()) { "Class $classId not found" }
            assertNotNull(provider.getClassLikeDeclarationByClassId(classId)) { "Class-like declaration $classId not found" }
        }

        for (directive in moduleStructure.allDirectives[Directives.TYPE_ALIAS]) {
            val classId = ClassId.fromString(directive)
            assert(provider.getAllTypeAliasesByClassId(classId).isNotEmpty()) { "Type alias $classId not found" }
            assertNotNull(provider.getClassLikeDeclarationByClassId(classId)) { "Class-like declaration $classId not found" }
        }

        for (directive in moduleStructure.allDirectives[Directives.FUNCTION]) {
            val callableId = parseCallableId(directive)
            assert(provider.getTopLevelFunctions(callableId).isNotEmpty()) { "Function $callableId not found" }
        }

        for (directive in moduleStructure.allDirectives[Directives.PROPERTY]) {
            val callableId = parseCallableId(directive)
            assert(provider.getTopLevelProperties(callableId).isNotEmpty()) { "Property $callableId not found" }
        }
    }

    private fun checkByVisitor(ktFile: KtFile, provider: KotlinFileBasedDeclarationProvider) {
        ktFile.accept(object : KtTreeVisitorVoid() {
            override fun visitClass(klass: KtClass) {
                super.visitClass(klass)
                processClassLikeDeclaration(klass)
            }

            override fun visitTypeAlias(typeAlias: KtTypeAlias) {
                super.visitTypeAlias(typeAlias)
                processClassLikeDeclaration(typeAlias)
            }

            private fun processClassLikeDeclaration(declaration: KtClassLikeDeclaration) {
                val classId = declaration.getClassId() ?: return
                val shortName = classId.shortClassName

                if (!classId.isNestedClass) {
                    assertContains(provider.getTopLevelKotlinClassLikeDeclarationNamesInPackage(classId.packageFqName), shortName)
                }

                when (declaration) {
                    is KtClassOrObject -> assertContains(provider.getAllClassesByClassId(classId), declaration)
                    is KtTypeAlias -> assertContains(provider.getAllTypeAliasesByClassId(classId), declaration)
                }
            }

            override fun visitNamedFunction(function: KtNamedFunction) {
                super.visitNamedFunction(function)
                processCallableDeclaration(function)
            }

            override fun visitProperty(property: KtProperty) {
                super.visitProperty(property)
                processCallableDeclaration(property)
            }

            private fun processCallableDeclaration(declaration: KtCallableDeclaration) {
                val callableId = declaration.callableId ?: return

                if (callableId.classId == null) {
                    assertContains(provider.getTopLevelCallableFiles(callableId), ktFile)
                    assertContains(provider.getTopLevelCallableNamesInPackage(callableId.packageName), callableId.callableName)

                    when (declaration) {
                        is KtFunction -> assertContains(provider.getTopLevelFunctions(callableId), declaration)
                        is KtProperty -> assertContains(provider.getTopLevelProperties(callableId), declaration)
                    }
                }
            }
        })
    }

    object Directives : SimpleDirectivesContainer() {
        val CLASS by stringDirective("ClassId of a class or object to be checked for presence")
        val TYPE_ALIAS by stringDirective("ClassId of a type alias to be checked for presence")
        val FUNCTION by stringDirective("CallableId of a function to be checked for presence")
        val PROPERTY by stringDirective("CallableId of a property to be checked for presence")
    }
}

private fun parseCallableId(rawString: String): CallableId {
    val chunks = rawString.split('#')
    assert(chunks.size == 2) { "Invalid CallableId string format: $rawString" }

    val rawQualifier = chunks[0]
    val rawCallableName = chunks[1]

    val callableName = Name.identifier(rawCallableName)

    return when {
        rawQualifier.endsWith('/') -> CallableId(FqName(rawQualifier.dropLast(1).replace('/', '.')), callableName)
        else -> CallableId(ClassId.fromString(rawQualifier, false), callableName)
    }
}

abstract class AbstractSourceFileBasedKotlinDeclarationProviderTest : AbstractFileBasedKotlinDeclarationProviderTest() {
    override val configurator = AnalysisApiFirSourceTestConfigurator(analyseInDependentSession = false)
}

abstract class AbstractScriptFileBasedKotlinDeclarationProviderTest : AbstractFileBasedKotlinDeclarationProviderTest() {
    override val configurator = AnalysisApiFirScriptTestConfigurator(analyseInDependentSession = false)
}