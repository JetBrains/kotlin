// FILE: A.kt

package aaa

import kotlin.jvm.*

public object TestObject {

    @JvmStatic
    public val test: String = "test"

}

// FILE: B.kt

fun main(args: Array<String>) {
    val h = aaa.TestObject.test
    if (h != "test") {
        throw Exception()
    }
}
