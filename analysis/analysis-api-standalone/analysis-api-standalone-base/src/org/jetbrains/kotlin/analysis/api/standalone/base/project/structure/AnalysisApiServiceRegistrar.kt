package org.jetbrains.kotlin.analysis.api.standalone.base.project.structure

import com.intellij.mock.MockApplication
import com.intellij.mock.MockProject
import com.intellij.openapi.Disposable

/**
 * @param DATA Additional information provided to the registrar by the setup process.
 */
interface AnalysisApiServiceRegistrar<in DATA> {
    fun registerApplicationServices(application: MockApplication, data: DATA)

    fun registerProjectExtensionPoints(project: MockProject, data: DATA)

    fun registerProjectServices(project: MockProject, data: DATA)

    fun registerProjectModelServices(project: MockProject, disposable: Disposable, data: DATA)
}

abstract class AnalysisApiSimpleServiceRegistrar : AnalysisApiServiceRegistrar<Any> {
    open fun registerApplicationServices(application: MockApplication) {}

    open fun registerProjectExtensionPoints(project: MockProject) {}

    open fun registerProjectServices(project: MockProject) {}

    open fun registerProjectModelServices(project: MockProject, disposable: Disposable) {}

    final override fun registerApplicationServices(application: MockApplication, data: Any) {
        registerApplicationServices(application)
    }

    final override fun registerProjectExtensionPoints(project: MockProject, data: Any) {
        registerProjectExtensionPoints(project)
    }

    final override fun registerProjectServices(project: MockProject, data: Any) {
        registerProjectServices(project)
    }

    final override fun registerProjectModelServices(project: MockProject, disposable: Disposable, data: Any) {
        registerProjectModelServices(project, disposable)
    }
}
