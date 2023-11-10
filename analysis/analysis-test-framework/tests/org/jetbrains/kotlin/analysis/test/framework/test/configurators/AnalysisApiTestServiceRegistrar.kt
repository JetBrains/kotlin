/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.test.framework.test.configurators

import com.intellij.mock.MockApplication
import com.intellij.mock.MockProject
import org.jetbrains.kotlin.test.services.TestServices

abstract class AnalysisApiTestServiceRegistrar {
    open fun registerApplicationServices(application: MockApplication, testServices: TestServices) {}

    open fun registerProjectExtensionPoints(project: MockProject, testServices: TestServices) {}

    open fun registerProjectServices(project: MockProject, testServices: TestServices) {}

    open fun registerProjectModelServices(project: MockProject, testServices: TestServices) {}
}
