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
        <!STUB_TYPE_IN_RECEIVER_CAUSES_AMBIGUITY!>a<!><!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE, OVERLOAD_RESOLUTION_AMBIGUITY, OVERLOAD_RESOLUTION_AMBIGUITY_BECAUSE_OF_STUB_TYPES!>++<!>
        bar(::a)
        if (<!USELESS_IS_CHECK, USELESS_IS_CHECK!>a is Int<!>) {
            a = 67
            <!STUB_TYPE_IN_RECEIVER_CAUSES_AMBIGUITY!>a<!><!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE, OVERLOAD_RESOLUTION_AMBIGUITY, OVERLOAD_RESOLUTION_AMBIGUITY_BECAUSE_OF_STUB_TYPES!>--<!>
            bar(::a)
        }
        when (condition) {
            true -> a = 87
            false -> a = 65
        }
        val x by <!DELEGATE_SPECIAL_FUNCTION_AMBIGUITY!>a<!>

        change {
            a = 99
        }
    }

    val value2 = myBuilder {
        accept("")
        a = <!TYPE_MISMATCH!>45<!>
        when (condition) {
            true -> a = <!TYPE_MISMATCH!>87<!>
            false -> a = <!TYPE_MISMATCH!>65<!>
        }
        change {
            a = <!TYPE_MISMATCH!>99<!>
        }
        if (<!USELESS_IS_CHECK, USELESS_IS_CHECK!>a is Int<!>) {
            a = <!TYPE_MISMATCH!>67<!>
        }
    }

    // See KT-54664
    val value3 = myBuilder {
        accept(<!TYPE_MISMATCH!>""<!>)
        a = 45
        bar(<!TYPE_MISMATCH!>::a<!>)
    }

    fun baz(t: Int) {}

    val value4 = myBuilder {
        accept("")
        a = 45
        b[0] = 123
        baz(<!TYPE_MISMATCH, TYPE_MISMATCH!>a<!>)
    }
}
