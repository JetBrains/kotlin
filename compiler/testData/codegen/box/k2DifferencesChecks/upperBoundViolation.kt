// ORIGINAL: /compiler/testData/diagnostics/testsWithStdLib/builderInference/upperBoundViolation.fir.kt
// WITH_STDLIB
// !RENDER_DIAGNOSTICS_FULL_TEXT
// ISSUE: KT-55055

fun <T : Number> printGenericNumber(t: T) = println("Number is $t")

fun main() {
    buildList { // inferred into MutableList<String>
        add("Boom")
        printGenericNumber(this[0])
    }
}


fun box() = "OK".also { main() }
