/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package common

import java.nio.file.Path
import java.nio.file.Paths

fun getServerEnv(): String = System.getenv("SERVER_ENV") ?: "localhost"
fun isServerEnvK8s(): Boolean = (System.getenv("SERVER_ENV") ?: "").contains("K8S")

const val CACHE_FOLDER_NAME = "cache"
const val ARTIFACTS_FOLDER_NAME = "artifacts"
const val TMP_FOLDER_NAME = "tmp"
const val STORAGE_FOLDER_NAME = "storage"
const val WORKSPACE_FOLDER_NAME = "workspace"

val K8S_STORAGE_PATH: Path = Paths.get("/$STORAGE_FOLDER_NAME")

val SERVER_CACHE_DIR: Path =
    if (isServerEnvK8s()) K8S_STORAGE_PATH.resolve(CACHE_FOLDER_NAME)
    else Paths.get(STORAGE_FOLDER_NAME, CACHE_FOLDER_NAME)

val SERVER_ARTIFACTS_CACHE_DIR: Path = SERVER_CACHE_DIR.resolve(ARTIFACTS_FOLDER_NAME)

val SERVER_TMP_CACHE_DIR: Path = SERVER_CACHE_DIR.resolve(TMP_FOLDER_NAME)

val SERVER_COMPILATION_WORKSPACE_DIR: Path =
    if (isServerEnvK8s()) K8S_STORAGE_PATH.resolve(WORKSPACE_FOLDER_NAME)
    else Paths.get(STORAGE_FOLDER_NAME, WORKSPACE_FOLDER_NAME)


val CLIENT_STORAGE_FOLDER_NAME = "client-storage"
val COMPILED_FOLDER_NAME = "compiled"

val CLIENT_COMPILED_DIR: Path = Paths.get(CLIENT_STORAGE_FOLDER_NAME, COMPILED_FOLDER_NAME)
val CLIENT_TMP_DIR: Path = Paths.get(CLIENT_STORAGE_FOLDER_NAME, TMP_FOLDER_NAME)

// TODO it would be appropriate to configure also different paths in k8s environment, do not forget to udpate Dockerfile
val AUTH_FILE: Path = Paths.get("src", "main", "kotlin", "server", "auth", "auth.json")
val UUID_FILE: Path = Paths.get("src", "main", "kotlin", "server", "auth", "uuid.json")
const val AUTH_KEY = "credential"
const val FOUR_MB = 4 * 1024 * 1024 - 1024 // minus safety margin