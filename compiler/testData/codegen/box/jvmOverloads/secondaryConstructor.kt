// IGNORE_BACKEND_FIR: JVM_IR
// TARGET_BACKEND: JVM

// WITH_RUNTIME

class C(val i: Int) {
    var status = "fail"

    @kotlin.jvm.JvmOverloads constructor(o: String, k: String = "K"): this(-1) {
        status = o + k
    }
}

fun box(): String {
    val c = (C::class.java.getConstructor(String::class.java).newInstance("O"))
    return c.status
}
