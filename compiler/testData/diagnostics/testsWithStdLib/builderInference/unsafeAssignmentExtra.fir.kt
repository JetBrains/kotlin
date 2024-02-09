// WITH_REFLECT
// FIR_DUMP
import kotlin.reflect.*

interface Foo<T : Any> {
    var a: T
    val b: Array<T>

    fun accept(arg: T)
}

<!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>class FooImpl<!><T : Any> : Foo<T>

fun bar(p: KMutableProperty0<Int>) {
    p.set(100)
}

fun <T : Any> myBuilder(block: Foo<T>.() -> Unit): Foo<T> = FooImpl<T>().apply(block)

fun <T : Any> Foo<T>.change(block: Foo<T>.() -> Unit): Foo<T> {
    this.block()
    return this
}

fun main(arg: Any, condition: Boolean) {
    val value = myBuilder {
        b[0] = 123
        a = 45
        a<!NONE_APPLICABLE!>++<!>
        bar(::a)
        if (<!USELESS_IS_CHECK!>a is Int<!>) {
            a = 67
            a<!NONE_APPLICABLE!>--<!>
            bar(::a)
        }
        when (condition) {
            true -> a = 87
            false -> a = 65
        }
        val x by <!DELEGATE_SPECIAL_FUNCTION_NONE_APPLICABLE!>a<!>

        change {
            a = 99
        }
    }

    val value2 = myBuilder {
        accept("")
        a = 45
        when (condition) {
            true -> a = 87
            false -> a = 65
        }
        change {
            a = 99
        }
        if (a is Int) {
            a = 67
        }
    }

    // See KT-54664
    val value3 = myBuilder {
        accept("")
        a = 45
        bar(::<!INAPPLICABLE_CANDIDATE!>a<!>)
    }

    fun baz(t: Int) {}

    val value4 = myBuilder {
        accept("")
        a = 45
        b[0] = 123
        baz(<!ARGUMENT_TYPE_MISMATCH!>a<!>)
    }
}
