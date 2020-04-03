// TARGET_BACKEND: JVM_IR

interface Foo

class Bar(f: Foo) : Foo by f {
    // Backing field is renamed to `$$delegate_0$1` in JVM_IR
    val `$$delegate_0`: Foo? = null
}

class Bar2(f: Foo) :
    // Backing field for delegate is renamed to `$$delegate_0$1` in JVM_IR
    Foo by f {

    lateinit var `$$delegate_0`: Foo
}