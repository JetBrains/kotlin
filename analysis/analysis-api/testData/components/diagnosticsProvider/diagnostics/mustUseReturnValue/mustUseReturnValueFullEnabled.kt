// WITH_STDLIB
// RETURN_VALUE_CHECKER_MODE: FULL
// COMPILER_ARGUMENTS: -Xreturn-value-checker=full
// MODULE: dep
// FILE: Dep.kt
class Unmarked {
    fun getStuff(): String = ""

    var prop: String = ""
        get() = field + ""
        set(value) {
            field = value
        }
}

fun toplvl(): String = ""

@MustUseReturnValues
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

// MODULE: main(dep)
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
