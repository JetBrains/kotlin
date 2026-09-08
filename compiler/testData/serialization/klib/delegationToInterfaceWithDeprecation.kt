// MODULE: lib
// FILE: lib.kt

package test

interface Some {
    @Deprecated("some" + "message", ReplaceWith("some" + "replacement"), DeprecationLevel.WARNING)
    fun foo()
}

// MODULE: main(lib)
// FILE: main.kt

package test

class Other(val s: Some) : Some by s
