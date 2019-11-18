// IGNORE_BACKEND_FIR: JVM_IR
class Outer {
    class Nested<T>(val t: T) {
        fun box() = t
    }
}

fun box(): String {
    if (Outer.Nested<String>("OK").box() != "OK") return "Fail"
    
    val x: Outer.Nested<String> = Outer.Nested("OK")
    return x.box()
}
