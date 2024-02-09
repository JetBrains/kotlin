// !LANGUAGE: +NoAdditionalErrorsInK1DiagnosticReporter
// ISSUE: KT-55055
fun <T : Number> printGenericNumber(t: T) = println("Number is $t")

fun main() {
    buildList { // inferred into MutableList<String>
        add("Boom")
        <!CANNOT_INFER_PARAMETER_TYPE, NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>printGenericNumber<!>(<!ARGUMENT_TYPE_MISMATCH!>this[0]<!>)
    }
}