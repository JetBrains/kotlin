/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.services

import org.jetbrains.kotlin.fir.util.ConeTypeRegistry
import org.jetbrains.kotlin.util.ArrayMapAccessor
import org.jetbrains.kotlin.util.ComponentArrayOwner
import org.jetbrains.kotlin.util.NullableArrayMapAccessor
import org.jetbrains.kotlin.util.TypeRegistry
import kotlin.reflect.KClass

interface TestService

data class ServiceRegistrationData(
    val kClass: KClass<out TestService>,
    val serviceConstructor: (TestServices) -> TestService
)

inline fun <reified T : TestService> service(
    noinline serviceConstructor: (TestServices) -> T
): ServiceRegistrationData {
    return ServiceRegistrationData(T::class, serviceConstructor)
}

class TestServices : ComponentArrayOwner<TestService, TestService>(){
    override val typeRegistry: TypeRegistry<TestService, TestService>
        get() = Companion

    companion object : ConeTypeRegistry<TestService, TestService>() {
        inline fun <reified T : TestService> testServiceAccessor(): ArrayMapAccessor<TestService, TestService, T> {
            return generateAccessor(T::class)
        }

        inline fun <reified T : TestService> nullableTestServiceAccessor(): NullableArrayMapAccessor<TestService, TestService, T> {
            return generateNullableAccessor(T::class)
        }
    }

    fun register(data: ServiceRegistrationData, skipAlreadyRegistered: Boolean) {
        if (skipAlreadyRegistered && getOrNull(data.kClass) != null) {
            return
        }
        registerComponent(data.kClass, data.serviceConstructor(this))
    }

    fun register(kClass: KClass<out TestService>, service: TestService) {
        registerComponent(kClass, service)
    }

    fun register(data: List<ServiceRegistrationData>, skipAlreadyRegistered: Boolean) {
        data.forEach { register(it, skipAlreadyRegistered) }
    }
}

fun TestServices.registerDependencyProvider(dependencyProvider: DependencyProvider) {
    register(DependencyProvider::class, dependencyProvider)
}
