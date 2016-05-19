/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

val f = File("C:\\Dev\\testProjects\\kotlin\\ChangeLog.md")

val map = hashMapOf<Int, Int>()

val m = f.forEachLine {
    line ->

    var str = line
    while (true) {
        val index = str.indexOf("KT-")
        if (index == -1) break

        val rest = str.substring(index + 3)

        try {
            val ticketNumber = Integer.parseInt(rest.takeWhile { it.isDigit() })
            map[ticketNumber] = (map[ticketNumber] ?: 0) + 1
            str = rest

        }
        catch (e: NumberFormatException) {
            break
        }
    }
}

map.forEach {
    if (it.value > 2) {
        print(it.key.toString() + " | ")
    }
}