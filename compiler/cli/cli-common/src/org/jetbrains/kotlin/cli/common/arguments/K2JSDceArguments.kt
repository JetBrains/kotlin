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
        @JvmStatic private val serialVersionUID = 0L
    }

    @Argument(
            value = "-output-dir",
            valueDescription = "<path>",
            description = "Output directory"
    )
    var outputDirectory: String? = null

    @Argument(
            value = "-keep",
            valueDescription = "<fully.qualified.name[,]>",
            description = "List of fully-qualified names of declarations that shouldn't be eliminated"
    )
    var declarationsToKeep: Array<String>? = null

    @Argument(
            value = "-Xprint-reachability-info",
            description = "Print declarations marked as reachable"
    )
    var printReachabilityInfo: Boolean = false
}
