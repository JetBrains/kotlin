// WITH_STDLIB

@JvmInline
value <!CONFLICTING_JVM_DECLARATIONS!>class A(val x: Int) {
    <!CONFLICTING_JVM_DECLARATIONS!>constructor(x: UInt): this(x.toInt())<!>
}<!>

data <!CONFLICTING_JVM_DECLARATIONS!>class B(val x: UInt) {
    <!CONFLICTING_JVM_DECLARATIONS!>constructor(x: Int) : this(x.toUInt())<!>
}<!>
