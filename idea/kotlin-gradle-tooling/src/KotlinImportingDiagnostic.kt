/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import java.io.Serializable


interface KotlinImportingDiagnostic : Serializable {
    fun deepCopy(cache: MutableMap<Any, Any>): KotlinImportingDiagnostic
}

typealias KotlinImportingDiagnosticsContainer = MutableSet<KotlinImportingDiagnostic>

interface KotlinSourceSetImportingDiagnostic : KotlinImportingDiagnostic {
    val kotlinSourceSet: KotlinSourceSet
}

data class OrphanSourceSetsImportingDiagnostic(override val kotlinSourceSet: KotlinSourceSet) : KotlinSourceSetImportingDiagnostic {
    override fun deepCopy(cache: MutableMap<Any, Any>): OrphanSourceSetsImportingDiagnostic =
        (cache[kotlinSourceSet] as? KotlinSourceSet)?.let { OrphanSourceSetsImportingDiagnostic(it) }
            ?: OrphanSourceSetsImportingDiagnostic(KotlinSourceSetImpl(kotlinSourceSet).apply { cache[kotlinSourceSet] = this })
}