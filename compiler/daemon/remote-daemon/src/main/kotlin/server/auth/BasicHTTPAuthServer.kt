/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package server.auth

import common.AUTH_FILE
import kotlinx.serialization.json.Json
import model.Credentials
import java.io.File

class BasicHTTPAuthServer : ServerAuth {

    private val allowedUsers = loadAllowed()

    private fun loadAllowed(): Set<String> {
        val jsonFile = File(AUTH_FILE)
        val json = Json { ignoreUnknownKeys = true; prettyPrint = true }
        return json.decodeFromString<Credentials>(jsonFile.readText()).allowed
    }

    override fun authenticate(credential: String): Boolean {
        return allowedUsers.contains(credential)
    }

}
