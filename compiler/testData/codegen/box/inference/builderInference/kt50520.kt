// WITH_STDLIB
// IGNORE_BACKEND_FIR: JVM_IR
// FIR status: different behavour with FE 1.0, reported `NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER`

fun box(): String {
    buildList {
        val foo = { first() }
        add(0, foo)
    }
    return "OK"
}