/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package common

//private suspend fun sendFileAsChunks(file: File, channel: Channel<CompileRequestGrpc>) {
//    val chunkSize = 64 * 1024
//    val fileBytes = file.readBytes()
//
//    fileBytes.asSequence()
//        .chunked(chunkSize)
//        .forEachIndexed { index, chunk ->
//            val fileChunk = FileChunkGrpc.newBuilder()
//                .setFileName(file.name)
//                .setContent(ByteString.copyFrom(chunk.toByteArray()))
//                .build()
//
//            val request = CompileRequestGrpc.newBuilder()
//                .setSourceFileChunk(fileChunk)
//                .build()
//            channel.send(request)
//        }
//}
