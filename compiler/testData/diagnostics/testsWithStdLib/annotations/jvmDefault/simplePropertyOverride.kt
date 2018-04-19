// !API_VERSION: 1.3
// !JVM_TARGET: 1.8
// !ENABLE_JVM_DEFAULT
interface A {
    @JvmDefault
    val test: String
        get() = "OK"
}

interface Abstract : A {
    <!JVM_DEFAULT_REQUIRED_FOR_OVERRIDE!>override val test: String<!>
}

interface ANonDefault {
    val test: String
        get() = "ANonDefault"
}

interface B: A {
    <!JVM_DEFAULT_REQUIRED_FOR_OVERRIDE!>override val test: String<!>
        get() = "B"
}

interface C: ANonDefault, A {
    <!JVM_DEFAULT_REQUIRED_FOR_OVERRIDE!>override val test: String<!>
        get() = "C"
}

interface D: A, ANonDefault {
    <!JVM_DEFAULT_REQUIRED_FOR_OVERRIDE!>override val test: String<!>
        get() = "C"
}