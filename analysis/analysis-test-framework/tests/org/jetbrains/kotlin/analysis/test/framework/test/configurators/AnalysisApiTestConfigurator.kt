/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.test.framework.test.configurators

import com.intellij.mock.MockApplication
import com.intellij.mock.MockProject
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.Application
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.analysis.api.standalone.base.project.structure.KtModuleProjectStructure
import org.jetbrains.kotlin.psi.NotNullableUserDataProperty
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestModuleStructure
import org.jetbrains.kotlin.test.services.TestServices
import java.nio.file.Path
import java.util.concurrent.locks.ReadWriteLock
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.withLock
import kotlin.reflect.KClass
import org.jetbrains.kotlin.analysis.providers.KotlinGlobalModificationService

abstract class AnalysisApiTestConfigurator {
    open val testPrefix: String? get() = null

    abstract val frontendKind: FrontendKind

    abstract val analyseInDependentSession: Boolean

    abstract fun configureTest(builder: TestConfigurationBuilder, disposable: Disposable)

    abstract val serviceRegistrars: List<AnalysisApiTestServiceRegistrar>

    open fun prepareFilesInModule(files: List<PsiFile>, module: TestModule, testServices: TestServices) {}

    open fun doGlobalModuleStateModification(project: Project) {
        KotlinGlobalModificationService.getInstance(project).publishGlobalModuleStateModification()
    }

    open fun computeTestDataPath(path: Path): Path = path

    abstract fun createModules(moduleStructure: TestModuleStructure, testServices: TestServices, project: Project): KtModuleProjectStructure

    fun registerProjectExtensionPoints(project: MockProject, testServices: TestServices) {
        serviceRegistrars.forEach { it.registerProjectExtensionPoints(project, testServices) }
    }

    fun registerProjectServices(project: MockProject, testServices: TestServices) {
        serviceRegistrars.forEach { it.registerProjectServices(project, testServices) }
    }

    fun registerProjectModelServices(project: MockProject, testServices: TestServices) {
        serviceRegistrars.forEach { it.registerProjectModelServices(project, testServices) }
    }

    fun registerApplicationServices(application: MockApplication, testServices: TestServices) {
        ApplicationServiceRegistrar.register(application, serviceRegistrars, testServices)
    }
}

object ApplicationServiceRegistrar {
    fun register(application: MockApplication, registrars: List<AnalysisApiTestServiceRegistrar>, testServices: TestServices) {
        val lock = application.lock
        for (registrar in registrars) {
            if (lock.readLock().withLock { application.isRegistrarRegistered(registrar) }) {
                continue
            }

            lock.writeLock().withLock {
                if (application.isRegistrarRegistered(registrar)) return@withLock
                registrar.registerApplicationServices(application, testServices)
                application.serviceRegistered[registrar::class] = true
            }
        }
    }

    private fun Application.isRegistrarRegistered(registrar: AnalysisApiTestServiceRegistrar): Boolean =
        serviceRegistered[registrar::class] == true

    private val Application.lock
            by NotNullableUserDataProperty<Application, ReadWriteLock>(
                Key("TestApplicationServicesRegistrarLock"),
                ReentrantReadWriteLock(),
            )

    private var Application.serviceRegistered
            by NotNullableUserDataProperty<Application, MutableMap<KClass<out AnalysisApiTestServiceRegistrar>, Boolean>>(
                Key("TestApplicationServicesRegistered"),
                mutableMapOf(),
            )
}
