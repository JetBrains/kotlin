// !JVM_TARGET: 1.8
// !JVM_DEFAULT_MODE: enable

interface A {
    @<!DEPRECATION!>JvmDefault<!>
    val test: String
        get() = "OK"
}

interface Abstract : A {
    override val test: String
}

interface ANonDefault {
    val test: String
        get() = "ANonDefault"
}

interface B: A {
    override val test: String
        get() = "B"
}

interface C: ANonDefault, A {
    override val test: String
        get() = "C"
}

interface D: A, ANonDefault {
    override val test: String
        get() = "C"
}
