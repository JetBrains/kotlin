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

package org.jetbrains.kotlin.daemon.common

import java.io.File
import java.rmi.RemoteException

interface IncrementalCompilerServicesFacade : CompilerServicesFacadeBase {
    // AnnotationFileUpdater
    @Throws(RemoteException::class)
    fun hasAnnotationsFileUpdater(): Boolean

    @Throws(RemoteException::class)
    fun updateAnnotations(outdatedClassesJvmNames: Iterable<String>)

    @Throws(RemoteException::class)
    fun revert()

    // ChangesRegistry
    @Throws(RemoteException::class)
    fun registerChanges(timestamp: Long, dirtyData: SimpleDirtyData)

    @Throws(RemoteException::class)
    fun unknownChanges(timestamp: Long)

    @Throws(RemoteException::class)
    fun getChanges(artifact: File, sinceTS: Long): Iterable<SimpleDirtyData>?
}