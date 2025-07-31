/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package client.auth

import kotlin.io.encoding.Base64

class BasicHTTPAuthClient(
    private val username: String,
    private val password: String
) : ClientAuth {


    override fun createCredential(): String {
        return Base64.Default.encode("$username:$password".toByteArray())
    }
}