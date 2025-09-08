/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.services

/**
 * Special marker service which allows understanding if CLI-based facades are enabled in tests.
 * It could be useful in some cases (e.g. in environment configurators)
 */
private object CliBasedFacadesMarker : TestService
private val TestServices.cliBasedFacadesMarker: CliBasedFacadesMarker? by TestServices.nullableTestServiceAccessor()

val cliBasedFacadesMarkerRegistrationData: ServiceRegistrationData
    get() = service { _: TestServices -> CliBasedFacadesMarker }

val TestServices.cliBasedFacadesEnabled: Boolean
    get() = cliBasedFacadesMarker != null
