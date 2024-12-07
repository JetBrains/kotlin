// RUN_PIPELINE_TILL: FRONTEND
// TARGET_BACKEND: JVM_IR

typealias ABoolean = Boolean
typealias AChar = Char
typealias AInt = Int
typealias ALong = Long
typealias AShort = Short
typealias AByte = Byte
typealias AFloat = Float
typealias ADouble = Double

fun main() {
    <!INVISIBLE_REFERENCE!>ABoolean<!>(false)
    <!INVISIBLE_REFERENCE!>Boolean<!>(false)

    <!INVISIBLE_REFERENCE!>AChar<!>('c')
    <!INVISIBLE_REFERENCE!>Char<!>('c')

    <!INVISIBLE_REFERENCE!>AInt<!>(42)
    <!INVISIBLE_REFERENCE!>Int<!>(42)

    <!INVISIBLE_REFERENCE!>ALong<!>(42)
    <!INVISIBLE_REFERENCE!>Long<!>(42)

    <!INVISIBLE_REFERENCE!>AShort<!>(42)
    <!INVISIBLE_REFERENCE!>Short<!>(42)

    <!INVISIBLE_REFERENCE!>AByte<!>(42)
    <!INVISIBLE_REFERENCE!>Byte<!>(42)

    <!INVISIBLE_REFERENCE!>AFloat<!>(4.2f)
    <!INVISIBLE_REFERENCE!>Float<!>(4.2f)

    <!INVISIBLE_REFERENCE!>ADouble<!>(4.2)
    <!INVISIBLE_REFERENCE!>Double<!>(4.2)
}
