/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package server.auth

import common.AUTH_FILE
import common.UUID_FILE
import kotlinx.serialization.json.Json
import model.Credentials
import model.UUIDMapping
import java.io.File
import java.util.UUID

class BasicHTTPAuthServer : ServerAuth {

    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }
    private val allowedUsers = loadAllowed()
    private val userUUIDs = loadUserUUIDs()


    private fun loadAllowed(): Set<String> {
        val jsonFile = AUTH_FILE.toFile()
        return json.decodeFromString<Credentials>(jsonFile.readText()).allowed
    }

    private fun loadUserUUIDs(): Map<String, String> {
        val jsonFile = UUID_FILE.toFile()
        return json.decodeFromString<List<UUIDMapping>>(jsonFile.readText())
            .associate { it.credential to it.userId }
    }

    override fun authenticate(credential: String): Boolean {
        return allowedUsers.contains(credential)
    }

    override fun getUserId(credential: String): String? {
        return userUUIDs[credential]
    }

}
