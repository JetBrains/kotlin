// WITH_STDLIB
// TARGET_BACKEND: JVM_IR

@JvmInline
value class A<!CONFLICTING_JVM_DECLARATIONS!>(val x: Int)<!> {
    <!CONFLICTING_JVM_DECLARATIONS!>constructor(x: UInt)<!>: this(x.toInt())
}

data class B<!CONFLICTING_JVM_DECLARATIONS!>(val x: UInt)<!> {
    <!CONFLICTING_JVM_DECLARATIONS!>constructor(x: Int)<!> : this(x.toUInt())
}
