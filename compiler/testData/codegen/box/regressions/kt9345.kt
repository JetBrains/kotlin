// IGNORE_BACKEND_FIR: JVM_IR
// TARGET_BACKEND: JVM
// WITH_REFLECT

// KT-9345 Type inference failure
fun Class<*>.foo(): Any? = kotlin.objectInstance


object OK {
    override fun toString(): String = "OK"
}

fun box(): String {
    return OK::class.java.foo().toString()
}