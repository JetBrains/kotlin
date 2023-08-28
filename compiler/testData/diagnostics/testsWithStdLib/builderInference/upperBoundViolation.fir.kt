// !RENDER_DIAGNOSTICS_FULL_TEXT
// ISSUE: KT-55055
// FIR_DUMP
fun <T : Number> printGenericNumber(t: T) = println("Number is $t")

fun main() {
    <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>buildList<!> { // inferred into MutableList<String>
        add("Boom")
        printGenericNumber(<!ARGUMENT_TYPE_MISMATCH!>this[0]<!>)
    }
}
