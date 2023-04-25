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
    <!INVISIBLE_MEMBER!>ABoolean<!>(<!TOO_MANY_ARGUMENTS!>false<!>)
    <!INVISIBLE_MEMBER!>Boolean<!>(<!TOO_MANY_ARGUMENTS!>false<!>)

    <!INVISIBLE_MEMBER!>AChar<!>(<!TOO_MANY_ARGUMENTS!>'c'<!>)
    <!INVISIBLE_MEMBER!>Char<!>(<!TOO_MANY_ARGUMENTS!>'c'<!>)

    <!INVISIBLE_MEMBER!>AInt<!>(<!TOO_MANY_ARGUMENTS!>42<!>)
    <!INVISIBLE_MEMBER!>Int<!>(<!TOO_MANY_ARGUMENTS!>42<!>)

    <!INVISIBLE_MEMBER!>ALong<!>(<!TOO_MANY_ARGUMENTS!>42<!>)
    <!INVISIBLE_MEMBER!>Long<!>(<!TOO_MANY_ARGUMENTS!>42<!>)

    <!INVISIBLE_MEMBER!>AShort<!>(<!TOO_MANY_ARGUMENTS!>42<!>)
    <!INVISIBLE_MEMBER!>Short<!>(<!TOO_MANY_ARGUMENTS!>42<!>)

    <!INVISIBLE_MEMBER!>AByte<!>(<!TOO_MANY_ARGUMENTS!>42<!>)
    <!INVISIBLE_MEMBER!>Byte<!>(<!TOO_MANY_ARGUMENTS!>42<!>)

    <!INVISIBLE_MEMBER!>AFloat<!>(<!TOO_MANY_ARGUMENTS!>4.2f<!>)
    <!INVISIBLE_MEMBER!>Float<!>(<!TOO_MANY_ARGUMENTS!>4.2f<!>)

    <!INVISIBLE_MEMBER!>ADouble<!>(<!TOO_MANY_ARGUMENTS!>4.2<!>)
    <!INVISIBLE_MEMBER!>Double<!>(<!TOO_MANY_ARGUMENTS!>4.2<!>)
}
