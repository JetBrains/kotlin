// IGNORE_BACKEND_FIR: JVM_IR
//KT-3189 Function invoke is called with no reason

fun box(): String {

    val bad = Bad({ 1 })

    return if (bad.test() == 1) "OK" else "fail"
}

class Bad(val a: () -> Int) {

    fun test(): Int = a()

    operator fun invoke(): Int = 2
}