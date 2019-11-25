// !LANGUAGE: +ProperComputationOrderOfTailrecDefaultParameters
// IGNORE_BACKEND_FIR: JVM_IR
// DONT_RUN_GENERATED_CODE: JS

var counter = 0
fun inc() = counter++

tailrec fun test(x: Int = 0, y: Int = inc(), z: Int = inc()) {
    if (x * 2 != y || z - y != 1)
        throw IllegalArgumentException("x=$x y=$y z=$z")

    if (x < 100000)
        test(x + 1)
}

fun box() : String {
    test()

    counter = 4
    test(x = 1, y = 2, z = 3)

    counter = 0
    test(-1, -2, -1)

    counter = 3
    test(1, 2)

    counter = 1
    test(y = 0)

    counter = 2
    test(x = 1)

    return "OK"
}