// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB

// MODULE: lib1

// FILE: Lib.kt
class Lib {
    fun getStuff(): String = ""

    class LibNested {
        fun getStuff2(): String = ""
    }

    inner class LibInner {
        fun getStuff3(): String = ""
    }
}

fun foo(): Lib.LibInner {
    val lib = Lib()
    <!RETURN_VALUE_NOT_USED!>lib.getStuff()<!>
    <!RETURN_VALUE_NOT_USED!>Lib.LibNested().getStuff2()<!>
    <!RETURN_VALUE_NOT_USED!>lib.LibInner().getStuff3()<!>
    return lib.LibInner()
}

// MODULE: main(lib1)

// FILE: App.kt

fun bar(): Lib.LibInner {
    val lib = Lib()
    <!RETURN_VALUE_NOT_USED!>lib.getStuff()<!>
    <!RETURN_VALUE_NOT_USED!>Lib.LibNested().getStuff2()<!>
    <!RETURN_VALUE_NOT_USED!>lib.LibInner().getStuff3()<!>
    return foo()
}

fun main() {
    <!RETURN_VALUE_NOT_USED!>bar()<!>
    val x = bar()
}
