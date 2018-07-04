// IGNORE_BACKEND: JVM_IR
class A {
    fun a () : String {
        class B() {
            fun s() : String = "OK"
        }
        return B().s()
    }
}

fun box() : String {
    return A().a()
}