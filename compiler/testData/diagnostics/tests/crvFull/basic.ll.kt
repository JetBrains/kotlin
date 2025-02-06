// LL_FIR_DIVERGENCE
// Apparently, LL(Reversed)DiagnosticsFe10TestGenerated does NOT run FE 1.0 despite its name,
// it runs FIR checkers as well (not sure why these tests are even generated for LL).
// However, it does not call FirCompilerRequiredAnnotationsResolveTransformer.transformFile to resolve annotations,
// and therefore @MustUseReturnValue annotations are not placed automatically.
// LL_FIR_DIVERGENCE

// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB
// FIR_DUMP

// MODULE: lib1

// FILE: Lib.kt
class Lib {
    fun getStuff(): String = ""
}

fun toplvl(): String = ""

@MustUseReturnValue
fun alreadyApplied(): String = ""

fun foo(): String {
    Lib()
    Lib().getStuff()
    toplvl()
    return Lib().getStuff()
}

// MODULE: main(lib1)

// FILE: App.kt

fun bar(): String {
    Lib()
    Lib().getStuff()
    foo()
    toplvl()
    return foo()
}

fun main() {
    bar()
    <!RETURN_VALUE_NOT_USED!>alreadyApplied()<!>
    val x = bar()
}
