/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.standalone.base.project.structure

import com.intellij.mock.MockApplication
import com.intellij.openapi.application.Application
import com.intellij.openapi.util.KeyWithDefaultValue
import com.intellij.openapi.util.UserDataHolder
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.withLock
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

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
    private val lock = ReentrantReadWriteLock()

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

    private val Application.serviceRegistered
            by UserDataPropertyWithDefault<Application, MutableMap<KClass<out Any>, Boolean>>(
                KeyWithDefaultValue.create("ApplicationServiceRegistration.servicesRegistered") { mutableMapOf() },
            )

    private class UserDataPropertyWithDefault<in R : UserDataHolder, T>(val key: KeyWithDefaultValue<T>) {
        operator fun getValue(thisRef: R, desc: KProperty<*>): T =
            thisRef.getUserData(key) ?: error("A user data key with a default value should guarantee a non-null value.")

        operator fun setValue(thisRef: R, desc: KProperty<*>, value: T) {
            thisRef.putUserData(key, value)
        }
    }
}
