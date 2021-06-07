// FIR_COMPARISON
fun globalFun(p: Int) {}

class C {
    fun foo() {
        val v = ::<caret>
    }
}

// ELEMENT: globalFun
