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

package org.jetbrains.kotlin.cli.common.arguments

class K2JSDceArguments : CommonToolArguments() {
    companion object {
        const val serialVersionUID = 0L
    }

    @field:GradleOption(DefaultValues.StringNullDefault::class)
    @field:Argument(value = "-output-dir", valueDescription = "<path>", description = "Output directory")
    @JvmField
    var outputDirectory: String? = null

    @field:GradleOption(DefaultValues.BooleanFalseDefault::class)
    @field:Argument(value = "-Xprint-reachability-info", description = "Print declarations marked as reachable")
    @JvmField
    var printReachabilityInfo: Boolean = false
}
