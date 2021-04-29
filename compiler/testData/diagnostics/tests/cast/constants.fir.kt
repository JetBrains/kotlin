fun asCall() {
    1 <!USELESS_CAST!>as Int<!>
    1 as Byte
    1 as Short
    1 as Long
    1 as Char
    1 as Double
    1 as Float

    1.0 as Int
    1.0 as Byte
    1.0 as Short
    1.0 as Long
    1.0 as Char
    1.0 <!USELESS_CAST!>as Double<!>
    1.0 as Float

    1f as Int
    1f as Byte
    1f as Short
    1f as Long
    1f as Char
    1f as Double
    1f <!USELESS_CAST!>as Float<!>
}

fun asSafe() {
    1 as? Int
    1 as? Byte
    1 as? Short
    1 as? Long
    1 as? Char
    1 as? Double
    1 as? Float

    1.0 as? Int
    1.0 as? Byte
    1.0 as? Short
    1.0 as? Long
    1.0 as? Char
    1.0 as? Double
    1.0 as? Float

    1f as? Int
    1f as? Byte
    1f as? Short
    1f as? Long
    1f as? Char
    1f as? Double
    1f as? Float
}
