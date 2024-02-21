// ISSUE: KT-57014
// FULL_JDK
// JVM_TARGET: 1.8

import java.util.function.Supplier

fun main() {
    val sam = Supplier<String> {
        foo()
    }
}

fun foo(): String? = null
