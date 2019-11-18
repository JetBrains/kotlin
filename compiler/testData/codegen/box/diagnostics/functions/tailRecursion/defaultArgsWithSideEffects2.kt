// !LANGUAGE: +ProperComputationOrderOfTailrecDefaultParameters
// IGNORE_BACKEND_FIR: JVM_IR
// Flag above doesn't matter cause 1 default value is passed explicitly in tail recursion call
// DONT_RUN_GENERATED_CODE: JS

var counter = 0
fun inc() = counter++

tailrec fun test(x: Int = 0, y: Int = inc(), z: Int = inc()) {
    if (x * 2 != y + 1 || z - y != -1)
        throw IllegalArgumentException("x=$x y=$y z=$z")

    if (x < 100000)
        test(z = inc(), x = (counter - 1)/2 + 1)
}

fun box() : String {
    counter = 4
    test(1, 1, 0)

    counter = 0
    test(-1, -3, -4)

    return "OK"
}