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

package org.jetbrains.kotlin.rmi

import java.io.File

public enum class OSKind {
    Windows,
    OSX,
    Unix,
    Unknown;

    companion object {
        val current: OSKind = System.getProperty("os.name").toLowerCase().let {
            when {
                // partly taken from http://www.code4copy.com/java/post/detecting-os-type-in-java
                it.startsWith("windows") -> OSKind.Windows
                it.startsWith("mac os") -> OSKind.OSX
                it.contains("unix") -> OSKind.Unix
                it.startsWith("linux") -> OSKind.Unix
                it.contains("bsd") -> OSKind.Unix
                it.startsWith("irix") -> OSKind.Unix
                it.startsWith("mpe/ix") -> OSKind.Unix
                it.startsWith("aix") -> OSKind.Unix
                it.startsWith("hp-ux") -> OSKind.Unix
                it.startsWith("sunos") -> OSKind.Unix
                it.startsWith("sun os") -> OSKind.Unix
                it.startsWith("solaris") -> OSKind.Unix
                else -> OSKind.Unknown
            }
        }
    }
}

private fun String?.orDefault(v: String): String =
    if (this == null || this.isBlank()) v else this

// Note links to OS recommendations for storing various kinds of files
// Windows: http://www.microsoft.com/security/portal/mmpc/shared/variables.aspx
// unix (freedesktop): http://standards.freedesktop.org/basedir-spec/basedir-spec-latest.html
// OS X: https://developer.apple.com/library/mac/documentation/FileManagement/Conceptual/FileSystemProgrammingGuide/AccessingFilesandDirectories/AccessingFilesandDirectories.html

public object FileSystem {

    val userHomePath: String get() = System.getProperty("user.home")
    val tempPath: String get() = System.getProperty("java.io.tmpdir")

    val logFilesPath: String get() = tempPath

    val runtimeStateFilesBasePath: String get() = when (OSKind.current) {
        OSKind.Windows -> System.getenv("LOCALAPPDATA").orDefault(tempPath)
        OSKind.OSX -> userHomePath + "/Library/Application Support"
        OSKind.Unix -> System.getenv("XDG_DATA_HOME").orDefault(userHomePath + "/.local/share")
        OSKind.Unknown -> tempPath
    }

    fun getRuntimeStateFilesPath(vararg names: String): String {
        assert(names.any())
        val base = File(runtimeStateFilesBasePath)
        // if base is not suitable, take home dir as a base and ensure the first name is prefixed with "." -
        //   this will work ok as a fallback solution on most systems
        val dir = if (base.exists() && base.isDirectory) names.fold(base, { r, v -> File(r, v) })
                  else names.drop(1)
                            .fold(File(userHomePath, names.first().let { if (it.startsWith(".")) it else ".$it" }),
                                  { r, v -> File(r, v)})
        return if ((dir.exists() && dir.isDirectory) || dir.mkdirs()) dir.absolutePath
               else tempPath
    }
}

