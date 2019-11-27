// IGNORE_BACKEND_FIR: JVM_IR
open class A<T> {
    open var x: T = "Fail" as T
    get
}

class B : A<String>() {
    override var x: String = "Fail"
    set
}

fun box(): String {
    val a: A<String> = B()
    a.x = "OK"
    return a.x
}
