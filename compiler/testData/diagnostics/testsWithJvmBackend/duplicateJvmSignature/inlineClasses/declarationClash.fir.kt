// COMPARE_WITH_LIGHT_TREE
// WITH_STDLIB

@JvmInline
value class A<!CONFLICTING_JVM_DECLARATIONS!>(val x: Int)<!> {
    <!CONFLICTING_JVM_DECLARATIONS{LT}!><!CONFLICTING_JVM_DECLARATIONS{PSI}!>constructor(x: UInt)<!>: this(x.toInt())<!>
}

data class B<!CONFLICTING_JVM_DECLARATIONS!>(val x: UInt)<!> {
    <!CONFLICTING_JVM_DECLARATIONS{LT}!><!CONFLICTING_JVM_DECLARATIONS{PSI}!>constructor(x: Int)<!> : this(x.toUInt())<!>
}
