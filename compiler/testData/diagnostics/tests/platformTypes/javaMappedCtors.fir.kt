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
    Boolean(false)

    <!INVISIBLE_REFERENCE!>AChar<!>('c')
    <!INVISIBLE_REFERENCE!>Char<!>('c')

    <!INVISIBLE_REFERENCE!>AInt<!>(42)
    <!INVISIBLE_REFERENCE!>Int<!>(42)

    <!INVISIBLE_REFERENCE!>ALong<!>(42)
    Long(42)

    <!INVISIBLE_REFERENCE!>AShort<!>(42)
    Short(42)

    <!INVISIBLE_REFERENCE!>AByte<!>(42)
    Byte(42)

    <!INVISIBLE_REFERENCE!>AFloat<!>(4.2f)
    Float(4.2f)

    <!INVISIBLE_REFERENCE!>ADouble<!>(4.2)
    Double(4.2)
}
