// FIR_COMPARISON
class C {
    fun foo() {
        val v = D::<caret>
    }
}

class D {
    fun memberFun(s: String){}
}

// ELEMENT: memberFun
