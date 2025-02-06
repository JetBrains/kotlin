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

    class LibNested {
        fun getStuff2(): String = ""
    }

    inner class LibInner {
        fun getStuff3(): String = ""
    }
}

fun foo(): Lib.LibInner {
    val lib = Lib()
    lib.getStuff()
    Lib.LibNested().getStuff2()
    lib.LibInner().getStuff3()
    return lib.LibInner()
}

// MODULE: main(lib1)

// FILE: App.kt

fun bar(): Lib.LibInner {
    val lib = Lib()
    lib.getStuff()
    Lib.LibNested().getStuff2()
    lib.LibInner().getStuff3()
    return foo()
}

fun main() {
    bar()
    val x = bar()
}
