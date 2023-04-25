// !RENDER_DIAGNOSTICS_FULL_TEXT
// ISSUE: KT-55055
// FIR_DUMP
fun <T : Number> printGenericNumber(t: T) = println("Number is $t")

fun main() {
    buildList { // inferred into MutableList<String>
        add("Boom")
        printGenericNumber(this[0])
    }
}
