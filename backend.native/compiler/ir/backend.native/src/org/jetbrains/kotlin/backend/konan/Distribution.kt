/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.konan.file.*
import org.jetbrains.kotlin.konan.properties.*
import org.jetbrains.kotlin.konan.target.*
import org.jetbrains.kotlin.konan.util.visibleName
import org.jetbrains.kotlin.konan.util.DependencyProcessor

class Distribution(val target: KonanTarget,
    propertyFileOverride: String? = null,
    runtimeFileOverride: String? = null) {

    val localKonanDir = "${File.userHome}/.konan"

    private fun findKonanHome(): String {
        val value = System.getProperty("konan.home", "dist")
        val path = File(value).absolutePath 
        return path
    }

    val konanHome = findKonanHome()
    val propertyFileName = propertyFileOverride ?: "$konanHome/konan/konan.properties"
    val properties = File(propertyFileName).loadProperties()

    val klib = "$konanHome/klib"
    val stdlib = "$klib/common/stdlib"

    val targetName = target.visibleName
    val defaultNatives = "$konanHome/konan/targets/${targetName}/native"
    val runtime = runtimeFileOverride ?: "$stdlib/targets/${targetName}/native/runtime.bc"

    val dependenciesDir = DependencyProcessor.defaultDependenciesRoot.absolutePath
}
