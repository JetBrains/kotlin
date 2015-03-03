// PARAM_TYPES: kotlin.String
// PARAM_DESCRIPTOR: val a: kotlin.String defined in main

fun main(args: Array<String>) {
    val a = "sdfsf"
    <selection>a.def(a = 1, b = "")</selection>
}

fun String.def(a: Int = 1, b: String) {}