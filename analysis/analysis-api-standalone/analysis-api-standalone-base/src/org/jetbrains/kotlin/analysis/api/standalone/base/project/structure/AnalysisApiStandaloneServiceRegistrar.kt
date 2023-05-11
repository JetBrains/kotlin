package org.jetbrains.kotlin.analysis.api.standalone.base.project.structure

import com.intellij.mock.MockApplication
import com.intellij.mock.MockProject
import com.intellij.openapi.Disposable

interface AnalysisApiStandaloneServiceRegistrar {
    fun registerApplicationServices(application: MockApplication)

    fun registerProjectExtensionPoints(project: MockProject)

    fun registerProjectServices(project: MockProject)

    fun registerProjectModelServices(project: MockProject, disposable: Disposable)
}