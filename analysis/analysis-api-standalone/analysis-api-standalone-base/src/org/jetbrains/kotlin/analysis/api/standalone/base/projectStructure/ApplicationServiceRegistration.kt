/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.standalone.base.projectStructure

import com.intellij.mock.MockApplication
import com.intellij.openapi.application.Application
import com.intellij.openapi.util.Key
import org.jetbrains.kotlin.psi.NotNullableUserDataProperty
import java.util.concurrent.locks.ReadWriteLock
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.withLock
import kotlin.reflect.KClass

/**
 * [ApplicationServiceRegistration] centralizes registration of services with shared [MockApplication]s.
 *
 * The Kotlin core environment shares the application between multiple tests running in parallel. We need to ensure that application
 * services are only registered once, and especially that no races happen. Thread safety is ensured with a lock, while registrations are
 * kept unique by remembering which service registrars have been applied already.
 *
 * This whole object is a workaround for improper application sharing, see [KT-64167](https://youtrack.jetbrains.com/issue/KT-64167).
 */
object ApplicationServiceRegistration {
    fun <DATA> register(application: MockApplication, registrars: List<AnalysisApiServiceRegistrar<DATA>>, data: DATA) {
        registerWithCustomRegistration(application, registrars) {
            registerApplicationServices(application, data)
        }
    }

    fun <DATA> registerWithCustomRegistration(
        application: MockApplication,
        registrars: List<AnalysisApiServiceRegistrar<DATA>>,
        register: AnalysisApiServiceRegistrar<DATA>.() -> Unit,
    ) {
        val lock = application.lock
        for (registrar in registrars) {
            if (lock.readLock().withLock { application.isRegistrarRegistered(registrar) }) {
                continue
            }

            lock.writeLock().withLock {
                if (application.isRegistrarRegistered(registrar)) return@withLock
                registrar.register()
                application.serviceRegistered[registrar::class] = true
            }
        }
    }

    private fun <DATA> Application.isRegistrarRegistered(registrar: AnalysisApiServiceRegistrar<DATA>): Boolean =
        serviceRegistered[registrar::class] == true

    private val Application.lock
            by NotNullableUserDataProperty<Application, ReadWriteLock>(
                Key("TestApplicationServicesRegistrarLock"),
                ReentrantReadWriteLock(),
            )

    private var Application.serviceRegistered
            by NotNullableUserDataProperty<Application, MutableMap<KClass<out Any>, Boolean>>(
                Key("TestApplicationServicesRegistered"),
                mutableMapOf(),
            )
}
