/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package common

import kotlinx.coroutines.flow.Flow
//import kotlinx.rpc.annotations.Rpc
import model.CompileRequest
import model.CompileResponse

//@Rpc
interface RemoteCompilationService {
    fun compile(compileRequests: Flow<CompileRequest>): Flow<CompileResponse>
    fun cleanup()
}

enum class RemoteCompilationServiceImplType {
    GRPC
}