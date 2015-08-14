class C {
    fun bar(){}
}

open class X {
    fun Any.foo() {
        if (this is C && this@X is Y) {
            <caret>
        }
    }
}

class Y : X() {
    fun y()
}

fun C.extFun(){}

// EXIST: bar
// EXIST: y
// EXIST: extFun
