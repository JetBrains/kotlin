// LANGUAGE: +ContextReceivers, -ContextParameters
// IGNORE_BACKEND_K2: ANY
// TARGET_BACKEND: JVM_IR

fun box(): String {
    with(A()) {
        object : B(){
            init {
                foo()
            }
        }
    }
    return "OK"
}

class A {
    fun foo() {}
}

context(A)
open class B