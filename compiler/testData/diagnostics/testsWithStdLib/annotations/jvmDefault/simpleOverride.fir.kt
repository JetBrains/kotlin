// !JVM_TARGET: 1.8
// !JVM_DEFAULT_MODE: enable

interface A {
    @<!DEPRECATION!>JvmDefault<!>
    fun test() {}
}

interface Abstract : A {
    override fun test()
}

interface ANonDefault {
    fun test() {}
}

interface B: A {
    override fun test() {}
}

interface C: ANonDefault, A {
    override fun test() {}
}

interface D: A, ANonDefault {
    override fun test() {}
}

class Foo : A {
    override fun test() {}
}
