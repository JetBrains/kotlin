// WITH_RUNTIME

import java.io.*

fun test(r: Reader) {
    val ss = hashSetOf<String>()
    r.useLines { it.forEach { ss.add(it) } }
}

// 2 POP
