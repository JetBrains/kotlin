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

fun main(args: Array<String>) {
    val filePathDefault = "padded-version.txt"

    if (args.isEmpty() || args.size > 2) {
        error("Usage: kotlinc -script counter-padding.kts" +
              " <version>" +
              " <file-path='$filePathDefault'>")
    }

    var versionStr = args[0]

    println("Version string: " + versionStr)

    val incrementPartStr = versionStr.takeLastWhile { it.isDigit() }
    val versionPrefix = versionStr.take(versionStr.length - incrementPartStr.length)

    val incrementPart = incrementPartStr.toInt()
    val padded = java.lang.String.format("%03d", incrementPart)

    var filePath = args.getOrNull(1) ?: filePathDefault

    val result = "${versionPrefix}$padded"

    println("prefix=$versionPrefix incrementPart=$incrementPart result=$padded filePath=$filePath")

    File("$filePath").writeText("$result")
}

main(args)