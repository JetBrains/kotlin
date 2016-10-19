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

package org.jetbrains.kotlin.config

class CompilerSettings {
    var additionalArguments: String = DEFAULT_ADDITIONAL_ARGUMENTS
    var copyJsLibraryFiles: Boolean = true
    var outputDirectoryForJsLibraryFiles: String = DEFAULT_OUTPUT_DIRECTORY

    constructor()

    constructor(settings: CompilerSettings) {
        additionalArguments = settings.additionalArguments
        scriptTemplates = settings.scriptTemplates
        scriptTemplatesClasspath = settings.scriptTemplatesClasspath
        copyJsLibraryFiles = settings.copyJsLibraryFiles
        outputDirectoryForJsLibraryFiles = settings.outputDirectoryForJsLibraryFiles
    }

    companion object {
        private val DEFAULT_ADDITIONAL_ARGUMENTS = "-version"
        private val DEFAULT_OUTPUT_DIRECTORY = "lib"
    }
}
