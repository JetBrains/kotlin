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

package org.jetbrains.kotlin.daemon.common

import java.io.File
import java.io.Serializable
import java.rmi.Remote
import java.rmi.RemoteException

interface IncrementalCompilationServicesFacade : Remote {
    @Throws(RemoteException::class)
    fun areFileChangesKnown(): Boolean

    @Throws(RemoteException::class)
    fun modifiedFiles(): List<File>?

    @Throws(RemoteException::class)
    fun deletedFiles(): List<File>?

    @Throws(RemoteException::class)
    fun workingDir(): File

    @Throws(RemoteException::class)
    fun customCacheVersionFileName(): String

    @Throws(RemoteException::class)
    fun customCacheVersion(): Int

    // ICReporter

    @Throws(RemoteException::class)
    fun shouldReportIC(): Boolean

    @Throws(RemoteException::class)
    fun reportIC(message: String)

    @Throws(RemoteException::class)
    fun reportCompileIteration(files: Iterable<File>, exitCode: Int)

    // AnnotationFileUpdater
    @Throws(RemoteException::class)
    fun hasAnnotationsFileUpdater(): Boolean

    @Throws(RemoteException::class)
    fun updateAnnotations(outdatedClassesJvmNames: Iterable<String>)

    @Throws(RemoteException::class)
    fun revert()
}