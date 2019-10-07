package com.jetbrains.cidr.apple.gradle

import AppleProjectExtension
import AppleSourceSet
import org.gradle.api.Project
import org.jetbrains.plugins.gradle.tooling.ErrorMessageBuilder
import org.jetbrains.plugins.gradle.tooling.ModelBuilderService
import java.lang.reflect.Proxy

class AppleProjectModelBuilder : ModelBuilderService {
    override fun getErrorMessageBuilder(project: Project, e: Exception): ErrorMessageBuilder =
        ErrorMessageBuilder.create(project, e, "Gradle import errors")
            .withDescription("Unable to build Apple Extensions plugin configuration")

    override fun canBuild(modelName: String?): Boolean = modelName == AppleProjectModel::class.java.name
    override fun buildAll(modelName: String?, project: Project): AppleProjectModel? {
        val projectExtension = project.extensions.findByName("apple")?.dynamic<AppleProjectExtension>() ?: return null
        val sourceSetsModels = mutableMapOf<String, AppleSourceSetModel>()
        for (value: Any in projectExtension.sourceSets) {
            with(value.dynamic<AppleSourceSet>()) {
                sourceSetsModels[name] = AppleSourceSetModelImpl(name, apple.srcDirs)
            }
        }
        return AppleProjectModelImpl(sourceSetsModels)
    }
}

private inline fun <reified T> Any.dynamic() =
    Proxy.newProxyInstance(T::class.java.classLoader, arrayOf(T::class.java)) { _, method, args ->
        (method.takeIf { this is T }
            ?: Class.forName(T::class.java.name, true, javaClass.classLoader).getMethod(method.name, *method.parameterTypes))
            .invoke(this, *(args ?: emptyArray()))
    } as T