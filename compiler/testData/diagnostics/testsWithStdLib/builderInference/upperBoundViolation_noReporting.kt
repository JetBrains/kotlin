// FIR_IDENTICAL
// !LANGUAGE: +NoAdditionalErrorsInK1DiagnosticReporter
// ISSUE: KT-55055
fun <T : Number> printGenericNumber(t: T) = println("Number is $t")

fun main() {
    buildList { // inferred into MutableList<String>
        add("Boom")
        printGenericNumber(this[0])
    }
}
