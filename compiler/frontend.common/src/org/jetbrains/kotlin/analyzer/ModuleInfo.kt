/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analyzer

import org.jetbrains.kotlin.descriptors.ModuleCapability
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.resolve.PlatformDependentAnalyzerServices

interface ModuleInfo {
    val name: Name
    val displayedName: String get() = name.asString()
    fun dependencies(): List<ModuleInfo>
    val expectedBy: List<ModuleInfo> get() = emptyList()
    val platform: TargetPlatform
    val analyzerServices: PlatformDependentAnalyzerServices
    fun modulesWhoseInternalsAreVisible(): Collection<ModuleInfo> = listOf()
    val capabilities: Map<ModuleCapability<*>, Any?>
        get() = mapOf(Capability to this)
    val stableName: Name?
        get() = null

    // For common modules, we add built-ins at the beginning of the dependencies list, after the SDK.
    // This is needed because if a JVM module depends on the common module, we should use JVM built-ins for resolution of both modules.
    // The common module usually depends on kotlin-stdlib-common which may or may not have its own (common, non-JVM) built-ins,
    // but if they are present, they should come after JVM built-ins in the dependencies list, because JVM built-ins contain
    // additional members dependent on the JDK
    fun dependencyOnBuiltIns(): DependencyOnBuiltIns = analyzerServices.dependencyOnBuiltIns()

    //TODO: (module refactoring) provide dependency on builtins after runtime in IDEA
    enum class DependencyOnBuiltIns { NONE, AFTER_SDK, LAST }

    companion object {
        val Capability = ModuleCapability<ModuleInfo>("ModuleInfo")
    }
}
