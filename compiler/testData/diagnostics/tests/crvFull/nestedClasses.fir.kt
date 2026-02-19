// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB

// MODULE: lib1

// FILE: Lib.kt
class Lib {
    fun getStuff(): String {
        fun localStuff(): String = ""
        <!RETURN_VALUE_NOT_USED!>localStuff<!>()
        return ""
    }

    class LibNested {
        fun getStuff2(): String = ""
    }

    inner class LibInner {
        fun getStuff3(): String = ""
    }
}

fun foo(): Lib.LibInner {
    val lib = Lib()
    lib.<!RETURN_VALUE_NOT_USED!>getStuff<!>()
    Lib.LibNested().<!RETURN_VALUE_NOT_USED!>getStuff2<!>()
    lib.LibInner().<!RETURN_VALUE_NOT_USED!>getStuff3<!>()
    return lib.LibInner()
}

// MODULE: main(lib1)

// FILE: App.kt

fun bar(): Lib.LibInner {
    val lib = Lib()
    lib.<!RETURN_VALUE_NOT_USED!>getStuff<!>()
    Lib.LibNested().<!RETURN_VALUE_NOT_USED!>getStuff2<!>()
    lib.LibInner().<!RETURN_VALUE_NOT_USED!>getStuff3<!>()
    return foo()
}

fun main() {
    <!RETURN_VALUE_NOT_USED!>bar<!>()
    val x = bar()
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, inner, localFunction, localProperty, nestedClass,
propertyDeclaration, stringLiteral */
