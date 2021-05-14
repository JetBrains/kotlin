// !LANGUAGE: -ProperComputationOrderOfTailrecDefaultParameters
// TARGET_BACKEND: JVM
// IGNORE_FIR_DIAGNOSTICS_DIFF

var counter = 0
fun calc(counter: Int) = if (counter % 2 == 0) "K" else "O"

<!TAILREC_WITH_DEFAULTS!>tailrec fun test(x: Int, y: String = calc(counter++), z: String = calc(counter++)): String<!> {
    if (x > 0)
        return y + z

    return test(x + 1)
}

fun box(): String {
    return test(0)
}
