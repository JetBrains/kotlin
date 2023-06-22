// COMPARE_WITH_LIGHT_TREE
// !LANGUAGE: +ValueClasses
// WITH_STDLIB

@JvmInline
value class A(val x: Int, val y: Int) {
    <!CONFLICTING_JVM_DECLARATIONS{LT}!><!CONFLICTING_JVM_DECLARATIONS{PSI}!>constructor(other: A)<!>: this(other.x, other.y)<!>
    <!CONFLICTING_JVM_DECLARATIONS{LT}!><!CONFLICTING_JVM_DECLARATIONS{PSI}!>constructor(x: UInt, y: UInt)<!>: this(x.toInt(), y.toInt())<!>
}

<!CONFLICTING_JVM_DECLARATIONS{LT}!>data class <!CONFLICTING_JVM_DECLARATIONS{PSI}!>B<!CONFLICTING_JVM_DECLARATIONS!>(val x: UInt, val y: UInt)<!><!> {
    <!CONFLICTING_JVM_DECLARATIONS{LT}!><!CONFLICTING_JVM_DECLARATIONS{PSI}!>constructor(other: A)<!> : this(other.x, other.y)<!>
    <!CONFLICTING_JVM_DECLARATIONS{LT}!><!CONFLICTING_JVM_DECLARATIONS{PSI}!>constructor(x: Int, y: Int)<!> : this(x.toUInt(), y.toUInt())<!>
}<!>
