// DONT_RUN_GENERATED_CODE: JS

object O {
    tailrec fun rec(i: Int) {
        if (i <= 0) return
        O.rec(i - 1)
    }
}

fun box(): String {
    O.rec(100000)
    return "OK"
}
