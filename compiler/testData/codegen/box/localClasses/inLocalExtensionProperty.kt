// IGNORE_BACKEND: JS_IR
package test

class C(val s : String) {
    val A.a: String
      get() {
        class B {
            val b : String
                get() = this@a.s + this@C.s
        }
        return B().b
    }

    fun test(a : A) : String {
        return a.a
    }
}

class A(val s: String) {
}

fun box() : String {
    return C("K").test(A("O"))
}