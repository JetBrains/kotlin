/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package server.grpc

import com.google.protobuf.Empty
import common.RemoteCompilationService
import common.toDomain
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import model.toProto
import org.jetbrains.kotlin.server.CompileRequestProto
import org.jetbrains.kotlin.server.CompileResponseProto
import org.jetbrains.kotlin.server.CompileServiceGrpcKt

class GrpcRemoteCompilationService(
    val impl: RemoteCompilationService,
) : CompileServiceGrpcKt.CompileServiceCoroutineImplBase() {

    override fun compile(requests: Flow<CompileRequestProto>): Flow<CompileResponseProto> {
        return impl.compile(requests.map { it.toDomain() }).map { it.toProto() }
    }

    override suspend fun cleanup(request: Empty): Empty {
        impl.cleanup()
        return Empty.getDefaultInstance()
    }
}