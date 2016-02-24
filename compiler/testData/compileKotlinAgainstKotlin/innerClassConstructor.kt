// FILE: A.kt

package second

public class Outer() {
    inner class Inner(test: String)
}

// FILE: B.kt

//test for KT-3702 Inner class constructor cannot be invoked in override function with receiver
import second.Outer

fun Outer.testExt() {
    Inner("test")
}

fun main(args: Array<String>) {
    Outer().testExt()
}
