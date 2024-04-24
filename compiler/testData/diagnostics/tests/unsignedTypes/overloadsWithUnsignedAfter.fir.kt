// LANGUAGE: +CorrectSpecificityCheckForSignedAndUnsigned
fun doStuff(value: Any) = "Any"
fun doStuff(value: UByte) = "UByte"

fun Any.doStuffExtension() = "Any"
fun UByte.doStuffExtension() = "UByte"

typealias TUByte = UByte

fun doStuffTa(value: Any) = "Any"
fun doStuffTa(value: TUByte) = "UByte"

fun doStuffComparable(value: Comparable<UByte>) = "Any"
fun doStuffComparable(value: UByte) = "UByte"

fun main() {
    val uByte: UByte = UByte.MIN_VALUE
    doStuff(uByte)
    uByte.doStuffExtension()
    doStuffTa(uByte)
    doStuffComparable(uByte)
}
