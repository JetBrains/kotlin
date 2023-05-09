// FIR_IDENTICAL
// !JVM_TARGET: 1.8
// !JVM_DEFAULT_MODE: all

interface A<T> {
    fun test(p: T) {
    }
}

@JvmDefaultWithCompatibility
interface ANonDefault {
    fun test(p: String) {}
}

interface B<T> : A<T> {

    override fun test(p: T)
    {}
}

interface C<T> : A<T>, ANonDefault {

    override fun test(p: T)
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

    override fun test(p: T)
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
