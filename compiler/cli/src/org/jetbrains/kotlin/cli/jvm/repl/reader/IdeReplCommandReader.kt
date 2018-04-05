/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.cli.jvm.repl.reader

import org.jetbrains.kotlin.cli.jvm.repl.ReplFromTerminal
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader

class IdeReplCommandReader : ReplCommandReader {
    override fun readLine(next: ReplFromTerminal.WhatNextAfterOneLine) = readLineForRepl()
    override fun flushHistory() = Unit
}

private val stdinForRepl: BufferedReader by lazy {
    BufferedReader(InputStreamReader(object : InputStream() {
        override fun read(): Int {
            return System.`in`.read()
        }

        override fun reset() {
            System.`in`.reset()
        }

        override fun read(b: ByteArray): Int {
            return System.`in`.read(b)
        }

        override fun close() {
            System.`in`.close()
        }

        override fun mark(readlimit: Int) {
            System.`in`.mark(readlimit)
        }

        override fun skip(n: Long): Long {
            return System.`in`.skip(n)
        }

        override fun available(): Int {
            return System.`in`.available()
        }

        override fun markSupported(): Boolean {
            return System.`in`.markSupported()
        }

        override fun read(b: ByteArray, off: Int, len: Int): Int {
            return System.`in`.read(b, off, len)
        }
    }))
}

private fun readLineForRepl(): String? = stdinForRepl.readLine()