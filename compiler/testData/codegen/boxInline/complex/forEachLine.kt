// WITH_STDLIB
// TARGET_BACKEND: JVM
// IGNORE_INLINER: IR
// FILE: 1.kt
package test

public class Input(val s1: String, val s2: String) {
    public fun iterator() : Iterator<String> {
        return arrayListOf(s1, s2).iterator()
    }
}

public inline fun <T, R> T.use(block: (T)-> R) : R {
    return block(this)
}

public inline fun Input.forEachLine(block: (String) -> Unit): Unit {
    useLines { lines -> lines.forEach(block) }
}

public inline fun Input.useLines(block2: (Iterator<String>) -> Unit): Unit {
        this.use{ block2(it.iterator()) }
}

// FILE: 2.kt

import test.*
import java.util.*


fun sample(): Input {
    return Input("Hello", "World");
}

fun testForEachLine() {
    val list = ArrayList<String>()
    val reader = sample()

    reader.forEachLine{
        list.add(it)
    }
}


fun box(): String {
    testForEachLine()

    return "OK"
}
