/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package common

import java.nio.file.Path
import java.nio.file.Paths

val SERVER_CACHE_DIR: Path = Paths.get("src", "main", "kotlin", "server", "cache")
val SERVER_ARTIFACTS_CACHE_DIR: Path = Paths.get("src", "main", "kotlin", "server", "cache", "artifacts")
val SERVER_TMP_CACHE_DIR: Path = Paths.get("src", "main", "kotlin", "server", "cache", "tmp")
val SERVER_COMPILATION_WORKSPACE_DIR: Path = Paths.get("src", "main", "kotlin", "server", "workspace")
val CLIENT_COMPILED_DIR: Path = Paths.get("src", "main", "kotlin", "client", "compiled")
val CLIENT_TMP_DIR: Path = Paths.get("src", "main", "kotlin", "client", "tmp")
val AUTH_FILE: Path = Paths.get("src", "main", "kotlin", "server", "auth", "auth.json")
val UUID_FILE: Path = Paths.get("src", "main", "kotlin", "server", "auth", "uuid.json")
const val AUTH_KEY = "credential"
const val FOUR_MB = 4 * 1024 * 1024 - 1024 // minus safety margin