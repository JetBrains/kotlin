// TARGET_BACKEND: JVM_OLD

interface Foo

class <!CONFLICTING_JVM_DECLARATIONS!>Bar(f: Foo)<!> : Foo by f {
    // Backing field is renamed to `$$delegate_0$1` in JVM_IR
    <!CONFLICTING_JVM_DECLARATIONS!>val `$$delegate_0`: Foo?<!> = null
}

class <!CONFLICTING_JVM_DECLARATIONS!>Bar2(f: Foo)<!> :
    // Backing field for delegate is renamed to `$$delegate_0$1` in JVM_IR
    Foo by f {

    <!CONFLICTING_JVM_DECLARATIONS!>lateinit var `$$delegate_0`: Foo<!>
}