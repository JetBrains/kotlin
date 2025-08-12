/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package server

import com.google.protobuf.Empty
import common.RemoteCompilationService
import common.toDomain
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import model.toGrpc
import org.jetbrains.kotlin.server.CompileRequestGrpc
import org.jetbrains.kotlin.server.CompileResponseGrpc
import org.jetbrains.kotlin.server.CompileServiceGrpcKt

class GrpcRemoteCompilationService(
    val impl: RemoteCompilationService,
) : CompileServiceGrpcKt.CompileServiceCoroutineImplBase() {

    override fun compile(requests: Flow<CompileRequestGrpc>): Flow<CompileResponseGrpc> {
        return impl.compile(requests.map { it.toDomain() }).map { it.toGrpc() }
    }

    override suspend fun cleanup(request: Empty): Empty {
        impl.cleanup()
        return Empty.getDefaultInstance()
    }
}