// !LANGUAGE: +ValueClasses
// WITH_STDLIB
// TARGET_BACKEND: JVM_IR

@JvmInline
value class A(val x: Int, val y: Int) {
    <!CONFLICTING_JVM_DECLARATIONS!>constructor(other: A)<!>: this(other.x, other.y)
    <!CONFLICTING_JVM_DECLARATIONS!>constructor(x: UInt, y: UInt)<!>: this(x.toInt(), y.toInt())
}

data class <!CONFLICTING_JVM_DECLARATIONS, CONFLICTING_JVM_DECLARATIONS!>B(val x: UInt, val y: UInt)<!> {
    <!CONFLICTING_JVM_DECLARATIONS!>constructor(other: A)<!> : this(other.x, other.y)
    <!CONFLICTING_JVM_DECLARATIONS!>constructor(x: Int, y: Int)<!> : this(x.toUInt(), y.toUInt())
}
