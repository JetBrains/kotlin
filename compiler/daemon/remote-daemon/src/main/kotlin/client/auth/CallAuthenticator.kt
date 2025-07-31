/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package client.auth

import common.AUTH_KEY
import io.grpc.CallCredentials
import io.grpc.Metadata
import io.grpc.Status
import java.util.concurrent.Executor

class CallAuthenticator(private val clientAuth: ClientAuth) : CallCredentials() {

    override fun applyRequestMetadata(
        requestInfo: RequestInfo,
        appExecutor: Executor,
        applier: MetadataApplier,
    ) {
        appExecutor.execute {
            try {
                val headers = Metadata()
                headers.put(Metadata.Key.of(AUTH_KEY, Metadata.ASCII_STRING_MARSHALLER), clientAuth.createCredential())
                applier.apply(headers)
            } catch (e: Throwable) {
                applier.fail(Status.UNAUTHENTICATED.withDescription("Failed to apply credentials: " + e.message))
            }
        }
    }

}