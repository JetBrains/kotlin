// FIR_IDENTICAL
// RUN_PIPELINE_TILL: BACKEND
// RENDER_DIAGNOSTICS_FULL_TEXT
interface A {
    fun b(a : Int)
}

interface B : A {}

class C1 : A {
    override fun b(b : Int) {}
}

class C2 : B {
    override fun b(b : Int) {}
}
