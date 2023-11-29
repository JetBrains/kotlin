/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.configurators

import com.intellij.core.CoreApplicationEnvironment
import com.intellij.mock.MockProject
import com.intellij.pom.PomModel
import com.intellij.pom.core.impl.PomModelImpl
import com.intellij.pom.tree.TreeAspect
import com.intellij.psi.PsiTreeChangeEvent
import com.intellij.psi.PsiTreeChangeListener
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.AnalysisApiTestServiceRegistrar
import org.jetbrains.kotlin.test.services.TestServices

/**
 * Registers project services which are required for PSI modification operations (such as deleting a PSI element) to complete without
 * throwing exceptions.
 *
 * @see AnalysisApiModifiablePsiTestApplicationEnvironmentConfiguration
 */
object AnalysisApiModifiablePsiTestServiceRegistrar : AnalysisApiTestServiceRegistrar() {
    override fun registerProjectExtensionPoints(project: MockProject, testServices: TestServices) {
        CoreApplicationEnvironment.registerExtensionPoint(
            project.extensionArea,
            PsiTreeChangeListener.EP.name,
            MockPsiTreeChangeListener::class.java,
        )
    }

    override fun registerProjectServices(project: MockProject, testServices: TestServices) {
        project.apply {
            registerService(TreeAspect::class.java)
            registerService(PomModel::class.java, PomModelImpl::class.java)
        }
    }
}

private class MockPsiTreeChangeListener : PsiTreeChangeListener {
    override fun beforeChildAddition(event: PsiTreeChangeEvent) {}

    override fun beforeChildRemoval(event: PsiTreeChangeEvent) {}

    override fun beforeChildReplacement(event: PsiTreeChangeEvent) {}

    override fun beforeChildMovement(event: PsiTreeChangeEvent) {}

    override fun beforeChildrenChange(event: PsiTreeChangeEvent) {}

    override fun beforePropertyChange(event: PsiTreeChangeEvent) {}

    override fun childAdded(event: PsiTreeChangeEvent) {}

    override fun childRemoved(event: PsiTreeChangeEvent) {}

    override fun childReplaced(event: PsiTreeChangeEvent) {}

    override fun childrenChanged(event: PsiTreeChangeEvent) {}

    override fun childMoved(event: PsiTreeChangeEvent) {}

    override fun propertyChanged(event: PsiTreeChangeEvent) {}
}
