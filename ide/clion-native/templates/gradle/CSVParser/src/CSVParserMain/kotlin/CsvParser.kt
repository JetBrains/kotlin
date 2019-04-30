/*
 * Copyright 2010-2019 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package sample.csvparser

import kotlinx.cinterop.*
import platform.posix.*

fun parseLine(line: String, separator: Char) : List<String> {
    val result = mutableListOf<String>()
    val builder = StringBuilder()
    var quotes = 0
    for (ch in line) {
        when {
            ch == '\"' -> {
                quotes++
                builder.append(ch)
            }
            (ch == '\n') || (ch ==  '\r') -> {}
            (ch == separator) && (quotes % 2 == 0) -> {
                result.add(builder.toString())
                builder.setLength(0)
            }
            else -> builder.append(ch)
        }
    }
    return result
}

fun main(args: Array<String>) {
    if (args.size != 3) {
        println("Usage: CSVParserApp.kexe <file.csv> <column> <count>")
        return
    }
    val fileName = args[0]
    val column = args[1].toInt()
    val count = args[2].toInt()

    val file = fopen(fileName, "r")
    if (file == null) {
        perror("cannot open input file $fileName")
        return
    }

    val keyValue = mutableMapOf<String, Int>()

    try {
        memScoped {
            val bufferLength = 64 * 1024
            val buffer = allocArray<ByteVar>(bufferLength)

            for (i in 1..count) {
                val nextLine = fgets(buffer, bufferLength, file)?.toKString()
                if (nextLine == null || nextLine.isEmpty()) break

                val records = parseLine(nextLine, ',')
                val key = records[column]
                val current = keyValue[key] ?: 0
                keyValue[key] = current + 1
            }
        }
    } finally {
        fclose(file)
    }

    keyValue.forEach {
        println("${it.key} -> ${it.value}")
    }
}
