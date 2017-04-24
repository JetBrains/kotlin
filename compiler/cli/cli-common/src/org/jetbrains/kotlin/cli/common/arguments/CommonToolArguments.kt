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

import com.intellij.util.SmartList
import java.io.Serializable

abstract class CommonToolArguments : Serializable {
    companion object {
        const val serialVersionUID = 0L
    }

    @field:JvmField
    var freeArgs: MutableList<String> = SmartList()

    @Transient
    @JvmField
    val errors = ArgumentParseErrors()

    @field:Argument(value = "-help", shortName = "-h", description = "Print a synopsis of standard options")
    @JvmField
    var help: Boolean = false

    @field:Argument(value = "-X", description = "Print a synopsis of advanced options")
    @JvmField
    var extraHelp: Boolean = false

    @field:Argument(value = "-version", description = "Display compiler version")
    @JvmField
    var version: Boolean = false

    @GradleOption(DefaultValues.BooleanFalseDefault::class)
    @field:Argument(value = "-verbose", description = "Enable verbose logging output")
    @JvmField
    var verbose: Boolean = false

    @GradleOption(DefaultValues.BooleanFalseDefault::class)
    @field:Argument(value = "-nowarn", description = "Generate no warnings")
    @JvmField
    var suppressWarnings: Boolean = false

    abstract fun executableScriptFileName(): String
}