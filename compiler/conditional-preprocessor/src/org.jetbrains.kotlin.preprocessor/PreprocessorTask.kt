/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.preprocessor

import org.apache.tools.ant.Task
import java.io.File

public class PreprocessorTask: Task() {

    public class JvmProfileConfig {
        public var version: Int? = null
        public var output: File? = null
    }
    public class JsProfileConfig{
        public var output: File? = null
    }

    public var src: File? = null

    private val profiles = arrayListOf<Profile>()

    public fun addConfiguredJvmProfile(configuration: JvmProfileConfig) {
        val version = configuration.version ?: throw IllegalArgumentException("version")
        val output = configuration.output ?: throw IllegalArgumentException("output")
        profiles.add(Profile("JVM$version", JvmPlatformEvaluator(version), output))
    }

    public fun addConfiguredJsProfile(configuration: JsProfileConfig) {
        val output = configuration.output ?: throw IllegalArgumentException("output")
        profiles.add(Profile("JS", JsPlatformEvaluator(), output))
    }

    override fun execute() {
        checkParameters()
        profiles.forEach { profile ->
            val preprocessor = Preprocessor()
            log("Preprocessing sources for ${profile.name}")
            preprocessor.processSources(src!!, profile)
        }
    }

    private fun checkParameters() {
        if (src == null) throw IllegalArgumentException("src")
        if (profiles.isEmpty()) throw IllegalArgumentException("profiles")
    }
}