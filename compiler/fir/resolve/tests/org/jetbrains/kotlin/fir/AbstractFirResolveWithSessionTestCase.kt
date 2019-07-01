/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import com.intellij.openapi.extensions.Extensions
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElementFinder
import org.jetbrains.kotlin.asJava.finder.JavaElementFinder
import org.jetbrains.kotlin.test.KotlinTestWithEnvironment

abstract class AbstractFirResolveWithSessionTestCase : KotlinTestWithEnvironment() {

    override fun setUp() {
        super.setUp()

        prepareProjectExtensions(project)
    }

    protected fun prepareProjectExtensions(project: Project) {
        Extensions.getArea(project)
            .getExtensionPoint(PsiElementFinder.EP_NAME)
            .unregisterExtension(JavaElementFinder::class.java)
    }

}
