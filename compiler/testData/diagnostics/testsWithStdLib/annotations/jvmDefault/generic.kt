// !API_VERSION: 1.3
// !JVM_TARGET: 1.8
// !ENABLE_JVM_DEFAULT
interface A<T> {
    @JvmDefault
    fun test(p: T) {
    }
}

interface ANonDefault {
    fun test(p: String) {}
}

interface B<T> : A<T> {

    <!JVM_DEFAULT_REQUIRED_FOR_OVERRIDE!>override fun test(p: T)<!>
    {}
}

interface C<T> : A<T>, ANonDefault {

    <!JVM_DEFAULT_REQUIRED_FOR_OVERRIDE!>override fun test(p: T)<!>
    {}

    override fun test(p: String) {
    }
}

interface C1 : C<String> {
    override fun test(p: String) {

    }
}

interface C2 : C<String>, ANonDefault {
    override fun test(p: String) {

    }
}

interface D<T> : ANonDefault, A<T> {

    <!JVM_DEFAULT_REQUIRED_FOR_OVERRIDE!>override fun test(p: T)<!>
    {}

    override fun test(p: String) {
    }
}

interface D1 : D<String> {
    override fun test(p: String) {

    }
}

interface D2 : ANonDefault, D<String> {
    override fun test(p: String) {

    }
}