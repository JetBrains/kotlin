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

import java.io.File

/**
 * Icrements last segement of version string: 0.12.1223 -> 0.12.1224 and writes result to file.
 */
fun main(args: Array<String>) {
    val filePathDefault = "updated-version.txt"

    if (args.isEmpty() || args.size > 2) {
        error("Usage: kotlinc -script increment-version.kts " +
              "<version> " +
              "<file-path='$filePathDefault'>")
    }

    var versionStr = args[0]

    val incrementPartStr = versionStr.takeLastWhile(Char::isDigit)
    val versionPrefix = versionStr.take(versionStr.length - incrementPartStr.length)
    val incrementPart = incrementPartStr.toInt()

    var filePath = args.getOrNull(1) ?: filePathDefault

    val result = "${versionPrefix}${incrementPart + 1}"

    println("prefix=$versionPrefix incrementPart=$incrementPart result=$result filePath=$filePath")

    File(filePath).writeText(result)
}

main(args)
