/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.resolve

import com.intellij.lang.java.JavaLanguage
import com.intellij.mock.MockProject
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.psi.impl.file.PsiPackageImpl
import com.intellij.psi.impl.light.LightMethodBuilder
import com.intellij.psi.impl.light.LightModifierList
import com.intellij.psi.impl.light.LightPsiClassBase
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.PsiSearchScopeUtil
import org.jetbrains.kotlin.analysis.api.standalone.base.projectStructure.AnalysisApiServiceRegistrar
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.DiagnosticCheckerFilter
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.collectDiagnosticsForFile
import org.jetbrains.kotlin.analysis.low.level.api.fir.withResolutionFacade
import org.jetbrains.kotlin.analysis.low.level.api.fir.test.configurators.AnalysisApiFirSourceTestConfigurator
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtTestModule
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.AnalysisApiTestServiceRegistrar
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.TestInfrastructureInternals
import org.jetbrains.kotlin.test.services.TestServices
import kotlin.test.fail

abstract class AbstractErrorResistanceTest : AbstractAnalysisApiBasedTest() {
    override val configurator = AnalysisApiFirSourceTestConfigurator(analyseInDependentSession = false)

    override val additionalServiceRegistrars: List<AnalysisApiServiceRegistrar<TestServices>>
        get() = super.additionalServiceRegistrars + listOf(ErrorResistanceServiceRegistrar)

    override fun doTestByMainFile(mainFile: KtFile, mainModule: KtTestModule, testServices: TestServices) {
        withResolutionFacade(mainFile) { resolutionFacade ->
            ENABLE_INTERRUPTION.set(true)

            try {
                mainFile.collectDiagnosticsForFile(resolutionFacade, DiagnosticCheckerFilter.ONLY_DEFAULT_CHECKERS)
                fail("Analysis should be interrupted")
            } catch (e: Throwable) {
                val errors = generateSequence(e) { it.cause }
                if (errors.none { it is AnalysisInterruptedException }) {
                    throw e
                }
            }

            ENABLE_INTERRUPTION.set(false)

            val diagnostics = mainFile.collectDiagnosticsForFile(resolutionFacade, DiagnosticCheckerFilter.ONLY_DEFAULT_CHECKERS)
            assert(diagnostics.isEmpty()) {
                val messages = diagnostics.map { it.factoryName }
                "There should be no diagnostics, found:\n" + messages.joinToString("\n")
            }
        }
    }
}

private object ErrorResistanceServiceRegistrar : AnalysisApiTestServiceRegistrar() {
    @OptIn(TestInfrastructureInternals::class)
    override fun registerProjectModelServices(project: MockProject, disposable: Disposable, testServices: TestServices) {
        with(PsiElementFinder.EP.getPoint(project)) {
            registerExtension(BrokenLibraryElementFinder(project), disposable)
        }
    }
}

private class AnalysisInterruptedException : RuntimeException()

private var ENABLE_INTERRUPTION = ThreadLocal.withInitial { false }

private fun interruptAnalysis() {
    if (ENABLE_INTERRUPTION.get()) {
        throw AnalysisInterruptedException()
    }
}

private class BrokenLibraryElementFinder(project: Project) : PsiElementFinder() {
    private val manager = PsiManager.getInstance(project)
    private val brokenPackage = BrokenPackage("broken.lib", manager)
    private val brokenClass = BrokenClass(brokenPackage.qualifiedName, "Foo", manager)

    override fun findPackage(qualifiedName: String): PsiPackage? {
        return when (qualifiedName) {
            brokenPackage.qualifiedName -> brokenPackage
            else -> null
        }
    }

    override fun findClass(qualifiedName: String, scope: GlobalSearchScope): PsiClass? {
        val klass = when (qualifiedName) {
            brokenClass.qualifiedName -> brokenClass
            else -> null
        }

        return klass?.takeIf { PsiSearchScopeUtil.isInScope(scope, it) }
    }

    override fun findClasses(qualifiedName: String, scope: GlobalSearchScope): Array<PsiClass> {
        val klass = findClass(qualifiedName, scope) ?: return PsiClass.EMPTY_ARRAY
        return arrayOf(klass)
    }
}

private class BrokenPackage(packageName: String, manager: PsiManager) : PsiPackageImpl(manager, packageName) {
    override fun isValid(): Boolean = true
}

private class BrokenClass(
    private val packageName: String,
    name: String,
    manager: PsiManager,
) : LightPsiClassBase(manager, JavaLanguage.INSTANCE, name) {
    private val modifierList: PsiModifierList = LightModifierList(manager, JavaLanguage.INSTANCE, PsiModifier.PUBLIC)
    private val methods: Array<PsiMethod> = arrayOf(ConstructorMethod(this), GetterMethod(this))

    override fun getQualifiedName(): String = "$packageName.$name"
    override fun getModifierList(): PsiModifierList = modifierList
    override fun getContainingClass(): PsiClass? = null
    override fun getTypeParameterList(): PsiTypeParameterList? = null

    override fun getMethods(): Array<PsiMethod> {
        return methods
    }

    override fun getFields(): Array<PsiField> {
        return PsiField.EMPTY_ARRAY
    }

    override fun getInnerClasses(): Array<PsiClass> = PsiClass.EMPTY_ARRAY
    override fun getInitializers(): Array<PsiClassInitializer> = PsiClassInitializer.EMPTY_ARRAY

    override fun getExtendsList(): PsiReferenceList? = null
    override fun getImplementsList(): PsiReferenceList? = null

    override fun getScope(): PsiElement? = null

    private class ConstructorMethod(owner: PsiClass) : LightMethodBuilder(owner, JavaLanguage.INSTANCE) {
        init {
            isConstructor = true
            setModifiers(PsiModifier.PUBLIC)

            val projectScope = GlobalSearchScope.allScope(manager.project)
            addParameter("first", PsiType.getJavaLangString(manager, projectScope))
            addParameter("second", PsiTypes.intType())
        }
    }

    private class GetterMethod(owner: PsiClass) : LightMethodBuilder(owner.manager, "getResult") {
        init {
            containingClass = owner
            setModifiers(PsiModifier.PUBLIC)
            setMethodReturnType(PsiTypes.booleanType())
        }

        override fun getTypeParameterList(): PsiTypeParameterList? {
            interruptAnalysis()
            return super.getTypeParameterList()
        }

        override fun getTypeParameters(): Array<PsiTypeParameter> {
            interruptAnalysis()
            return super.getTypeParameters()
        }
    }
}