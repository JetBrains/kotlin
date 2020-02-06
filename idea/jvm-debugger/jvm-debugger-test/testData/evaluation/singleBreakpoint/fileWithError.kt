// FILE: fileWithError.kt
package fileWithError

fun main(args: Array<String>) {
    // There is an error about internal visibility while analyzing fileWithInternal.kt
    fileWithInternal.test()
}

// ADDITIONAL_BREAKPOINT: fileWithInternal.kt / Breakpoint

// EXPRESSION: 1
// RESULT: 1: I

// FILE: lib/fileWithInternal.kt
package fileWithInternal

fun test() {
    // Breakpoint
    val a = fileWithInternal2.MyInternal()
}

// FILE: lib/fileWithInternal2.kt
package fileWithInternal2

internal class MyInternal