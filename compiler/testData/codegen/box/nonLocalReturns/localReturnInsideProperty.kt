// IGNORE_BACKEND_FIR: JVM_IR
interface ClassData

fun f() = object : ClassData {
    val someInt: Int
        get() {
            return 5
        }
}

fun box(): String{
    f()
    return "OK"
}