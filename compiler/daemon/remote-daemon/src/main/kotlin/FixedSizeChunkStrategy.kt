import com.google.protobuf.ByteString
import kotlinx.coroutines.channels.Channel
import org.jetbrains.kotlin.server.CompileRequestGrpc
import org.jetbrains.kotlin.server.FileChunkGrpc
import java.io.File
import kotlin.collections.toByteArray
import kotlin.sequences.chunked
import kotlin.sequences.forEachIndexed

/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

class FixedSizeChunkStrategy{

}

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
