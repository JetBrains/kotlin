/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package common

import java.nio.file.Path
import java.nio.file.Paths

fun getServerEnv(): String = System.getenv("SERVER_ENV") ?: "localhost"
fun isServerEnvK8s(): Boolean = (System.getenv("SERVER_ENV") ?: "").contains("K8S")

val K8S_STORAGE_PATH: Path = Paths.get("/storage")

val SERVER_CACHE_DIR: Path =
    if (isServerEnvK8s()) K8S_STORAGE_PATH.resolve("cache")
    else Paths.get("src", "main", "kotlin", "server", "cache")

val SERVER_ARTIFACTS_CACHE_DIR: Path = SERVER_CACHE_DIR.resolve("artifacts")

val SERVER_TMP_CACHE_DIR: Path = SERVER_CACHE_DIR.resolve("tmp")

val SERVER_COMPILATION_WORKSPACE_DIR: Path =
    if (isServerEnvK8s()) K8S_STORAGE_PATH.resolve("workspace")
    else Paths.get("src", "main", "kotlin", "server", "workspace")


val CLIENT_COMPILED_DIR: Path = Paths.get("src", "main", "kotlin", "client", "compiled")
val CLIENT_TMP_DIR: Path = Paths.get("src", "main", "kotlin", "client", "tmp")

// TODO it would be appropriate to configure also different paths in k8s environment, do not forget to udpate Dockerfile
val AUTH_FILE: Path = Paths.get("src", "main", "kotlin", "server", "auth", "auth.json")
val UUID_FILE: Path = Paths.get("src", "main", "kotlin", "server", "auth", "uuid.json")
const val AUTH_KEY = "credential"
const val FOUR_MB = 4 * 1024 * 1024 - 1024 // minus safety margin