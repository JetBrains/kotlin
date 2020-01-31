package com.jetbrains.cidr.apple.gradle

import AppleProjectExtension
import AppleSourceSet
import AppleTarget
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
        val targetModels = mutableMapOf<String, AppleTargetModel>()
        for (value: Any in projectExtension.targets) {
            val target = value.dynamic<AppleTarget>()
            val srcDirs = target.sourceSet.dynamic<AppleSourceSet>().apple.srcDirs
            val testDirs = target.testSourceSet.dynamic<AppleSourceSet>().apple.srcDirs
            val bridgingHeader = target.bridgingHeader?.let { bridgingHeader ->
                srcDirs.firstOrNull { it.isDirectory && it.exists() }?.resolve(bridgingHeader)
            }
            val buildDir = project.buildDir.resolve("apple").resolve(target.name)
            target.name.let { name -> targetModels[name] = AppleTargetModelImpl(name, srcDirs, testDirs, buildDir, bridgingHeader) }
        }
        return AppleProjectModelImpl(targetModels)
    }
}

private inline fun <reified T> Any.dynamic(): T = dynamic(T::class.java)

@Suppress("UNCHECKED_CAST")
private fun <T> Any.dynamic(clazz: Class<T>): T =
    Proxy.newProxyInstance(clazz.classLoader, arrayOf(clazz)) { _, method, args ->
        val targetMethod = method.takeIf { clazz.isInstance(it) }
            ?: Class.forName(clazz.name, true, javaClass.classLoader).getMethod(method.name, *method.parameterTypes)
        val returnValue = targetMethod.invoke(this, *(args ?: emptyArray()))
        when {
            returnValue == null || method.returnType.isInstance(returnValue) -> returnValue
            else -> returnValue.dynamic(method.returnType)
        }
    } as T