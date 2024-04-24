// FIR_IDENTICAL
fun doStuff(value: Any) = "Any"
fun doStuff(value: UByte) = "UByte"

fun Any.doStuffExtension() = "Any"
fun UByte.doStuffExtension() = "UByte"

typealias TUByte = UByte

fun doStuffTa(value: Any) = "Any"
fun doStuffTa(value: TUByte) = "UByte"

fun main() {
    val uByte: UByte = UByte.MIN_VALUE
    <!OVERLOAD_RESOLUTION_AMBIGUITY!>doStuff<!>(uByte)
    uByte.<!OVERLOAD_RESOLUTION_AMBIGUITY!>doStuffExtension<!>()
    <!OVERLOAD_RESOLUTION_AMBIGUITY!>doStuffTa<!>(uByte)
}
