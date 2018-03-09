// !API_VERSION: 1.3
// !JVM_TARGET: 1.8
@JvmDefaultFeature
interface A {
    @JvmDefault
    fun test() {
    }
}

interface Abstract : <!EXPERIMENTAL_API_USAGE!>A<!> {
    <!JVM_DEFAULT_REQUIRED_FOR_OVERRIDE!>override fun <!EXPERIMENTAL_OVERRIDE!>test<!>()<!>
}

interface ANonDefault {
    fun test() {}
}

interface B : <!EXPERIMENTAL_API_USAGE!>A<!> {
    <!JVM_DEFAULT_REQUIRED_FOR_OVERRIDE!>override fun <!EXPERIMENTAL_OVERRIDE!>test<!>()<!> {}
}

interface C : ANonDefault, <!EXPERIMENTAL_API_USAGE!>A<!> {
    <!JVM_DEFAULT_REQUIRED_FOR_OVERRIDE!>override fun <!EXPERIMENTAL_OVERRIDE!>test<!>()<!> {}
}

interface D : <!EXPERIMENTAL_API_USAGE!>A<!>, ANonDefault {
    <!JVM_DEFAULT_REQUIRED_FOR_OVERRIDE!>override fun <!EXPERIMENTAL_OVERRIDE!>test<!>()<!> {}
}