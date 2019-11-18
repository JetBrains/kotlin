// IGNORE_BACKEND_FIR: JVM_IR
public open class Outer private constructor(val s: String) {

    companion object {
        fun test () =  { Outer("OK") }()
    }
}

fun box(): String {
    return Outer.test().s
}