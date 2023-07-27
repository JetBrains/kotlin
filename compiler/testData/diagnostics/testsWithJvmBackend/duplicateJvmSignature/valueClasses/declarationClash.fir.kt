// !LANGUAGE: +ValueClasses
// WITH_STDLIB

@JvmInline
value class A(val x: Int, val y: Int) {
    <!CONFLICTING_JVM_DECLARATIONS!>constructor(other: A): this(other.x, other.y)<!>
    <!CONFLICTING_JVM_DECLARATIONS!>constructor(x: UInt, y: UInt): this(x.toInt(), y.toInt())<!>
}

<!CONFLICTING_JVM_DECLARATIONS!>data class B<!CONFLICTING_JVM_DECLARATIONS!>(val x: UInt, val y: UInt)<!> {
    <!CONFLICTING_JVM_DECLARATIONS!>constructor(other: A) : this(other.x, other.y)<!>
    <!CONFLICTING_JVM_DECLARATIONS!>constructor(x: Int, y: Int) : this(x.toUInt(), y.toUInt())<!>
}<!>
