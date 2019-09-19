/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization.nextgen

import org.junit.Test
import org.jetbrains.kotlin.backend.common.serialization.proto.IrClass as ProtoClass
import org.jetbrains.kotlin.backend.common.serialization.proto.IrFunction as ProtoFunction
import org.jetbrains.kotlin.backend.common.serialization.proto.IrStatement as ProtoStatement

class ProtoBenchmarks {

    enum class MeasureUnits(val delimeter: Long, private val suffix: String) {
        NANOSECONDS(1L, "ns"),
        MICROSECONDS(1000L, "mcs"),
        MILLISECONDS(1000L * 1000L, "ms");

        fun convert(nanos: Long): String = "${(nanos / delimeter)}$suffix"
    }

    private fun runBenchWithWarmup(name: String, W: Int, N: Int, butchSize: Int, measurer: MeasureUnits, wmpDone: () -> Unit = {}, bnhDone: (Long) -> Unit = {}, pre: () -> Unit = {}, bench: () -> Unit) {

        println("Run $name benchmark" + if (butchSize != 0) " (butch size = $butchSize)" else "")

        println("Warmup: $W times...")

        repeat(W) {
            println("W: ${it + 1} out of $W")
            pre()
            repeat(butchSize) {
                bench()
            }
        }

        var total = 0L

        wmpDone()

        println("Run bench: $N times...")

        repeat(N) {
            print("R: ${it + 1} out of $N ")
            pre()
            val start = System.nanoTime()
            repeat(butchSize) {
                bench()
            }
            val iter = System.nanoTime() - start
            println("takes ${measurer.convert(iter / butchSize)}")
            bnhDone(iter)
            total += iter
        }

        println("$name takes ${measurer.convert(total / (N * butchSize))}")
    }


    private fun String.asBytes(): ByteArray {
        return this.split(" ").map {
            it.toInt(radix = 16).toByte()
        }.toByteArray()
    }

    // class Comparable
    private val classByteString = "a 14 a 2 8 3 12 4 12 2 8 b 1a 6 8 bb 7 10 96 a 22 0 12 2 8 a 18 2 22 4 a 2 8 c 28 4 30 0 38 0 40 0 48 0 50 0 5a 2d a 14 a 2 8 15 12 4 12 2 8 18 1a 6 8 bb 7 10 96 a 22 0 12 2 8 11 18 ff ff ff ff ff ff ff ff ff 1 22 2 8 5 30 0 38 0 62 26 a 24 a 14 a 2 8 4 12 4 12 2 8 b 1a 6 8 d7 7 10 db 7 22 0 12 2 8 d 18 0 20 0 2a 2 8 2 30 0 6a f0 3 a 8a 1 32 87 1 a 7f a 14 a 2 8 6 12 4 12 2 8 b 1a 6 8 f8 9 10 94 a 22 0 12 2 8 f 1a 4 a 2 8 c 20 0 28 0 32 0 3a 2d a 14 a 2 8 8 12 4 12 2 8 b 1a 6 8 e8 9 10 94 a 22 0 12 2 8 11 18 ff ff ff ff ff ff ff ff ff 1 22 2 8 5 30 0 38 0 4a 24 a 14 a 2 8 9 12 4 12 2 8 b 1a 6 8 86 a 10 8e a 22 0 12 2 8 12 18 0 22 2 8 4 30 0 38 0 5a 2 8 3 10 4 18 0 20 0 a 8e 1 32 8b 1 a 7f a 14 a 2 8 a 12 4 12 2 8 14 1a 6 8 c2 7 10 96 a 22 0 12 2 8 13 1a 4 a 2 8 c 20 0 28 0 32 0 3a 2d a 14 a 2 8 c 12 4 12 2 8 b 1a 6 8 bb 7 10 96 a 22 0 12 2 8 11 18 ff ff ff ff ff ff ff ff ff 1 22 2 8 7 30 0 38 0 4a 24 a 14 a 2 8 d 12 4 12 2 8 b 1a 6 8 c2 7 10 96 a 22 0 12 2 8 12 18 0 22 2 8 2 30 0 38 0 5a 2 8 6 10 3 18 0 20 0 2a 2 8 e a 67 32 65 a 59 a 14 a 2 8 f 12 4 12 2 8 14 1a 6 8 c2 7 10 96 a 22 0 12 2 8 16 1a 4 a 2 8 c 20 0 28 0 32 0 3a 2d a 14 a 2 8 10 12 4 12 2 8 b 1a 6 8 bb 7 10 96 a 22 0 12 2 8 11 18 ff ff ff ff ff ff ff ff ff 1 22 2 8 7 30 0 38 0 5a 2 8 3 10 3 18 0 20 0 2a 2 8 11 a 67 32 65 a 59 a 14 a 2 8 12 12 4 12 2 8 14 1a 6 8 c2 7 10 96 a 22 0 12 2 8 17 1a 4 a 2 8 c 20 0 28 0 32 0 3a 2d a 14 a 2 8 13 12 4 12 2 8 b 1a 6 8 bb 7 10 96 a 22 0 12 2 8 11 18 ff ff ff ff ff ff ff ff ff 1 22 2 8 7 30 0 38 0 5a 2 8 0 10 3 18 0 20 0 2a 2 8 14 72 2 8 7"
    private val classBytes = classByteString.asBytes()

    // fun isInterfaceImpl
    private val headerByteString = "a 7a a 14 a 2 8 26 12 4 12 2 8 2 1a 6 8 af 3 10 a4 8 22 0 12 2 8 1c 1a 4 a 2 8 4 20 0 28 0 32 0 4a 24 a 14 a 2 8 27 12 4 12 2 8 2 1a 6 8 c3 3 10 cd 3 22 0 12 2 8 1d 18 0 22 2 8 0 30 0 38 0 4a 24 a 14 a 2 8 28 12 4 12 2 8 2 1a 6 8 cf 3 10 dd 3 22 0 12 2 8 1e 18 1 22 2 8 b 30 0 38 0 52 2 8 0 5a 2 8 4 10 1 18 0 20 0"
    private val headerBytes = headerByteString.asBytes()

    private val bodyByteString = "a 6 8 e8 3 10 a4 8 22 ad 11 a ae 1 a 6 8 ee 3 10 8d 4 1a a3 1 a 94 1 da 1 90 1 a 87 1 a 6 8 ee 3 10 8d 4 2a 7d a 50 a 42 1a 40 a 2 8 2a 12 34 1a 17 a 15 a 7 82 1 4 a 2 8 27 12 2 8 0 1a 6 8 f2 3 10 f6 3 1a 17 a 15 a 7 82 1 4 a 2 8 28 12 2 8 b 1a 6 8 fb 3 10 80 4 22 0 22 4 a 2 8 23 12 2 8 4 1a 6 8 f2 3 10 80 4 12 29 a 1b 9a 1 18 a 2 8 26 12 12 a 4 32 2 10 1 12 2 8 4 1a 6 8 89 4 10 8d 4 12 2 8 d 1a 6 8 82 4 10 8d 4 12 4 a 2 8 20 12 2 8 c 1a 6 8 ee 3 10 8d 4 a 67 a 6 8 93 4 10 b3 4 12 5d 4a 5b a 14 a 2 8 2c 12 4 12 2 8 2 1a 6 8 93 4 10 b3 4 22 0 12 2 8 25 1a 2 8 9 20 0 28 0 30 0 3a 35 a 27 1a 25 a 2 8 19 12 19 a 15 a 7 82 1 4 a 2 8 27 12 2 8 0 1a 6 8 a2 4 10 a6 4 22 0 22 4 a 2 8 26 12 2 8 9 1a 6 8 a7 4 10 b3 4 a fd 6 a 6 8 b8 4 10 83 6 1a f2 6 a e3 6 da 1 df 6 a d6 6 a 6 8 b8 4 10 83 6 2a cb 6 a 6d a 5f 1a 5d a 2 8 2d 12 51 a 4d a 3f 1a 3d a 2 8 2e 12 31 1a 17 a 15 a 7 82 1 4 a 2 8 2c 12 2 8 9 1a 6 8 bc 4 10 c4 4 1a 14 a 12 a 4 32 2 8 1 12 2 8 e 1a 6 8 c8 4 10 cc 4 22 0 22 4 a 2 8 28 12 2 8 4 1a 6 8 bc 4 10 cc 4 22 0 22 4 a 2 8 28 12 2 8 4 1a 6 8 bc 4 10 cc 4 12 d9 5 a ca 5 a c7 5 12 67 a 6 8 d8 4 10 fc 4 12 5d 4a 5b a 14 a 2 8 2f 12 4 12 2 8 2 1a 6 8 d8 4 10 fc 4 22 0 12 2 8 5 1a 2 8 1 20 0 28 0 30 0 3a 35 a 27 1a 25 a 2 8 2 12 19 a 15 a 7 82 1 4 a 2 8 2c 12 2 8 9 1a 6 8 e9 4 10 f1 4 22 0 22 4 a 2 8 26 12 2 8 1 1a 6 8 f2 4 10 fc 4 12 db 4 a 6 8 85 5 10 fd 5 1a d0 4 a c1 4 a be 4 a 4 a 2 8 2a 12 67 a 6 8 8f 5 10 99 5 12 5d 4a 5b a 14 a 2 8 30 12 4 12 2 8 2b 1a 6 8 8f 5 10 99 5 22 0 12 2 8 2c 1a 2 8 f 20 0 28 0 30 0 3a 35 a 27 1a 25 a 2 8 32 12 19 a 15 a 7 82 1 4 a 2 8 2f 12 2 8 1 1a 6 8 8f 5 10 99 5 22 0 22 4 a 2 8 2b 12 2 8 f 1a 6 8 8f 5 10 99 5 12 cc 3 a 6 8 85 5 10 fd 5 1a c1 3 a b2 3 e2 1 ae 3 a ab 3 8 0 12 35 a 27 1a 25 a 2 8 33 12 19 a 15 a 7 82 1 4 a 2 8 30 12 2 8 f 1a 6 8 8f 5 10 99 5 22 0 22 4 a 2 8 31 12 2 8 4 1a 6 8 8f 5 10 99 5 22 e9 2 a da 2 a d7 2 a 4 a 2 8 32 12 67 a 6 8 8a 5 10 8b 5 12 5d 4a 5b a 14 a 2 8 34 12 4 12 2 8 33 1a 6 8 8a 5 10 8b 5 22 0 12 2 8 34 1a 2 8 0 20 0 28 0 30 0 3a 35 a 27 1a 25 a 2 8 35 12 19 a 15 a 7 82 1 4 a 2 8 30 12 2 8 f 1a 6 8 8f 5 10 99 5 22 0 22 4 a 2 8 36 12 2 8 0 1a 6 8 8f 5 10 99 5 12 e5 1 a 6 8 9b 5 10 fd 5 1a da 1 a cb 1 a c8 1 12 c5 1 a 6 8 a9 5 10 f3 5 1a ba 1 a ab 1 da 1 a7 1 a 9e 1 a 6 8 a9 5 10 f3 5 2a 93 1 a 4a a 3c 1a 3a a 2 8 26 12 34 1a 17 a 15 a 7 82 1 4 a 2 8 34 12 2 8 0 1a 6 8 bd 5 10 be 5 1a 17 a 15 a 7 82 1 4 a 2 8 28 12 2 8 b 1a 6 8 c0 5 10 c5 5 22 0 12 2 8 4 1a 6 8 ad 5 10 c6 5 12 45 a 37 a 35 12 33 a 6 8 da 5 10 e5 5 1a 29 a 1b 9a 1 18 a 2 8 26 12 12 a 4 32 2 10 1 12 2 8 4 1a 6 8 e1 5 10 e5 5 12 2 8 d 1a 6 8 da 5 10 e5 5 12 2 8 d 1a 6 8 c8 5 10 f3 5 12 4 a 2 8 20 12 2 8 c 1a 6 8 a9 5 10 f3 5 12 2 8 c 1a 6 8 9b 5 10 fd 5 12 2 8 c 1a 6 8 85 5 10 fd 5 2a 4 a 2 8 32 12 2 8 c 1a 6 8 85 5 10 fd 5 12 2 8 c 1a 6 8 85 5 10 fd 5 12 2 8 c 1a 6 8 ce 4 10 83 6 12 4 a 2 8 20 12 2 8 c 1a 6 8 b8 4 10 83 6 a b8 3 a 6 8 89 6 10 ef 6 12 ad 3 4a aa 3 a 14 a 2 8 36 12 4 12 2 8 2 1a 6 8 89 6 10 ef 6 22 0 12 2 8 37 1a 2 8 b 20 0 28 0 30 0 3a 83 3 a f4 2 da 1 f0 2 a b3 2 a 6 8 9e 6 10 ef 6 2a a8 2 a 8d 1 a 7f 1a 7d a 2 8 2d 12 71 a 6d a 5f 1a 5d a 2 8 2e 12 51 1a 37 a 35 a 27 1a 25 a 2 8 1c 12 19 a 15 a 7 82 1 4 a 2 8 27 12 2 8 0 1a 6 8 a2 6 10 a6 6 22 0 22 4 a 2 8 26 12 2 8 a 1a 6 8 a7 6 10 b0 6 1a 14 a 12 a 4 32 2 8 1 12 2 8 e 1a 6 8 b4 6 10 b8 6 22 0 22 4 a 2 8 28 12 2 8 4 1a 6 8 a2 6 10 b8 6 22 0 22 4 a 2 8 28 12 2 8 4 1a 6 8 a2 6 10 b8 6 12 95 1 a 86 1 f2 1 82 1 8 1e 12 47 a 39 ea 1 36 a 2 8 38 12 30 a 22 1a 20 a 2 8 37 12 1a 1a 16 a 14 a 6 32 4 52 2 8 39 12 2 8 8 1a 6 8 be 6 10 c4 6 22 0 12 2 8 b 1a 6 8 ba 6 10 c6 6 12 2 8 b 1a 6 8 c7 6 10 e5 6 1a 35 a 27 1a 25 a 2 8 1c 12 19 a 15 a 7 82 1 4 a 2 8 27 12 2 8 0 1a 6 8 d6 6 10 da 6 22 0 22 4 a 2 8 26 12 2 8 a 1a 6 8 db 6 10 e4 6 12 2 8 b 1a 6 8 c7 6 10 e5 6 a 32 a 6 8 9e 6 10 ef 6 2a 28 a 12 a 4 32 2 10 1 12 2 8 4 1a 6 8 9e 6 10 ef 6 12 12 a 4 32 2 8 1 12 2 8 e 1a 6 8 eb 6 10 ef 6 12 4 a 2 8 20 12 2 8 b 1a 6 8 9e 6 10 ef 6 a 97 2 a 6 8 f4 6 10 d2 7 12 8c 2 4a 89 2 a 14 a 2 8 38 12 4 12 2 8 2 1a 6 8 f4 6 10 d2 7 22 0 12 2 8 3a 1a 2 8 a 20 0 28 0 30 0 3a e2 1 a d3 1 da 1 cf 1 a 92 1 a 6 8 92 7 10 d2 7 2a 87 1 a 3e a 30 f2 1 2d 8 12 12 15 a 7 82 1 4 a 2 8 36 12 2 8 b 1a 6 8 96 7 10 a4 7 1a 12 a 4 32 2 8 1 12 2 8 e 1a 6 8 a8 7 10 ac 7 12 2 8 4 1a 6 8 96 7 10 ac 7 12 45 a 37 ca 1 34 8 a 12 2 8 a 1a 2c a 1e ea 1 1b a 2 8 3b 12 15 a 7 82 1 4 a 2 8 36 12 2 8 b 1a 6 8 ae 7 10 bc 7 12 2 8 b 1a 6 8 bd 7 10 c8 7 12 2 8 a 1a 6 8 bd 7 10 c8 7 a 32 a 6 8 92 7 10 d2 7 2a 28 a 12 a 4 32 2 10 1 12 2 8 4 1a 6 8 92 7 10 d2 7 12 12 a 4 32 2 8 1 12 2 8 e 1a 6 8 ce 7 10 d2 7 12 4 a 2 8 20 12 2 8 a 1a 6 8 92 7 10 d2 7 a bb 2 a 6 8 d7 7 10 a2 8 1a b0 2 a a1 2 9a 1 9d 2 a 2 8 26 12 96 2 a 87 2 da 1 83 2 a c6 1 a 6 8 de 7 10 f6 7 2a bb 1 a 6d a 5f 1a 5d a 2 8 2d 12 51 a 4d a 3f 1a 3d a 2 8 2e 12 31 1a 17 a 15 a 7 82 1 4 a 2 8 38 12 2 8 a 1a 6 8 de 7 10 ee 7 1a 14 a 12 a 4 32 2 8 1 12 2 8 e 1a 6 8 f2 7 10 f6 7 22 0 22 4 a 2 8 28 12 2 8 4 1a 6 8 de 7 10 f6 7 22 0 22 4 a 2 8 28 12 2 8 4 1a 6 8 de 7 10 f6 7 12 4a a 3c 1a 3a a 2 8 26 12 34 1a 17 a 15 a 7 82 1 4 a 2 8 38 12 2 8 a 1a 6 8 8a 8 10 9a 8 1a 17 a 15 a 7 82 1 4 a 2 8 28 12 2 8 b 1a 6 8 9c 8 10 a1 8 22 0 12 2 8 4 1a 6 8 fa 7 10 a2 8 a 32 a 6 8 fa 7 10 a2 8 2a 28 a 12 a 4 32 2 10 1 12 2 8 4 1a 6 8 fa 7 10 a2 8 12 12 a 4 32 2 10 0 12 2 8 4 1a 6 8 fa 7 10 a2 8 12 4 a 2 8 3c 12 2 8 4 1a 6 8 de 7 10 a2 8 12 2 8 d 1a 6 8 d7 7 10 a2 8"
    private val bodyBytes = bodyByteString.asBytes()

    @Test
    fun testClassProto() {
        runBenchWithWarmup("Parse class bytes with Default Proto Parser", 50, 30, 15000, MeasureUnits.MICROSECONDS) {
            ProtoClass.parseFrom(classBytes)
        }
    }

    @Test
    fun testFunctionHeaderProto() {
        runBenchWithWarmup("Parse function header bytes with Default Proto Parser", 50, 30, 15000, MeasureUnits.MICROSECONDS) {
            ProtoFunction.parseFrom(headerBytes)
        }
    }

    @Test
    fun testFunctionBodyProto() {
        runBenchWithWarmup("Parse function body bytes with Default Proto Parser", 50, 30, 15000, MeasureUnits.MICROSECONDS) {
            ProtoStatement.parseFrom(bodyBytes)
        }
    }

    @Test
    fun testClassNextgen() {
        runBenchWithWarmup("Parse class bytes with Nextgen Parser", 50, 30, 15000, MeasureUnits.MICROSECONDS) {
//            SmartIrProtoReaderImpl(classBytes).readIrClass()
        }
    }

    @Test
    fun testFunctionHeaderNextgen() {
        runBenchWithWarmup("Parse function header bytes with Nextgen Parser", 50, 30, 15000, MeasureUnits.NANOSECONDS) {
//            SmartIrProtoReaderImpl(headerBytes).readIrFunction()
        }
    }

    @Test
    fun testFunctionBodyNextgen() {
        runBenchWithWarmup("Parse function body bytes with Nextgen Parser", 50, 30, 15000, MeasureUnits.MICROSECONDS) {
//            SmartIrProtoReaderImpl(bodyBytes).readIrStatement()
        }
    }
}