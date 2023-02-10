/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.psi.stubs

object KotlinStubVersions {

    // Source stub version should be increased if stub format (org.jetbrains.kotlin.psi.stubs.impl) is changed
    //      or changes are made to the parser that can potentially modify the psi structure of kotlin source code.
    // Though only kotlin declarations (no code in the bodies) are stubbed, please do increase this version
    //      if you are not 100% sure it can be avoided.
    // Increasing this version will lead to reindexing of all kotlin source files on the first IDE startup with the new version.
    const val SOURCE_STUB_VERSION = 146

    // Binary stub version should be increased if stub format (org.jetbrains.kotlin.psi.stubs.impl) is changed
    //      or changes are made to the core stub building code (org.jetbrains.kotlin.idea.decompiler.stubBuilder).
    // Increasing this version will lead to reindexing of all binary files that are potentially kotlin binaries (including all class files).
    private const val BINARY_STUB_VERSION = 82

    // Classfile stub version should be increased if changes are made to classfile stub building subsystem (org.jetbrains.kotlin.idea.decompiler.classFile)
    // Increasing this version will lead to reindexing of all classfiles.
    const val CLASSFILE_STUB_VERSION = BINARY_STUB_VERSION + 0

    // BuiltIn stub version should be increased if changes are made to builtIn stub building subsystem (org.jetbrains.kotlin.idea.decompiler.builtIns)
    // Increasing this version will lead to reindexing of all builtIn files (see KotlinBuiltInFileType).
    const val BUILTIN_STUB_VERSION = BINARY_STUB_VERSION + 3

    // JS stub version should be increased if changes are made to js stub building subsystem (org.jetbrains.kotlin.idea.decompiler.js)
    // Increasing this version will lead to reindexing of js binary files (see KotlinJavaScriptMetaFileType).
    const val JS_STUB_VERSION = BINARY_STUB_VERSION + 3
}
