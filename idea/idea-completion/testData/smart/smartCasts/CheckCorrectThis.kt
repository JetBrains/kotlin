open class A(val v1: Any?, val v2: Any?, val v3: Any?)

class B(v1: Any?, v2: Any?, v3: Any?, val v4: Any?) : A(v1, v2, v3)

class C(val c: Any?) {
    fun B.foo() {
        fun A.bar() {
            if (this@foo.v1 is String && this@bar.v2 is String && this.v3 is String && this@foo.v4 is String && this@C.c is String) {
                f(<caret>)
            }
        }
    }
}

fun f(s: String){}

// ABSENT: v1
// EXIST: v2
// EXIST: v3
// EXIST: v4
// EXIST: c