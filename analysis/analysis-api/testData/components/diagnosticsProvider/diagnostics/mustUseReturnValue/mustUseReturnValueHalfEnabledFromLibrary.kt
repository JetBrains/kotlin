// WITH_STDLIB
// RETURN_VALUE_CHECKER_MODE: CHECKER
// COMPILER_ARGUMENTS: -Xreturn-value-checker=check
// MODULE: lib
// MODULE_KIND: LibraryBinary
// FILE: Lib.kt
class Unmarked {
    fun getStuff(): String = ""

    var prop: String = ""
        get() = field + ""
        set(value) {
            field = value
        }
}

fun toplvl(): String = ""

@MustUseReturnValue
class Marked {
    fun alreadyApplied(): String = ""

    var prop: String = ""
        get() = field + ""
        set(value) {
            field = value
        }
}

enum class E {
    A, B;
    fun foo() = ""
}

// MODULE: main(lib)
// FILE: main.kt

fun foo(): String {
    Unmarked()
    Unmarked().getStuff()
    Unmarked().prop
    Unmarked().prop = ""
    toplvl()
    Marked().alreadyApplied()
    Marked().prop
    E.A.foo()
    E.A
    return Unmarked().getStuff()
}
