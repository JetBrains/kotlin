// IGNORE_BACKEND_FIR: JVM_IR
// DONT_RUN_GENERATED_CODE: JS

fun test() {
    tailrec fun g3(counter : Int) {
        if (counter > 0) { g3(counter - 1) }
    }
    g3(1000000)
}

fun box() : String {
    test()
    return "OK"
}