// !DIAGNOSTICS: -UNUSED_PARAMETER, -UNUSED_TYPEALIAS_PARAMETER

@Target(AnnotationTarget.TYPE)
annotation class Anno

class Foo1<T1<!UNSUPPORTED_FEATURE!>,<!>>

class Foo2<
        T1,
        T2: T1<!UNSUPPORTED_FEATURE!>,<!>
        > {
    fun <T1,
            T2<!UNSUPPORTED_FEATURE!>,<!> > foo2() {}

    internal inner class B<T,T2<!UNSUPPORTED_FEATURE!>,<!>>
}

interface A<T<!UNSUPPORTED_FEATURE!>,<!>>

interface A1<T<!UNSUPPORTED_FEATURE!>,<!>/**/>

fun <T<!UNSUPPORTED_FEATURE!>,<!>> foo1() {}

fun <T<!UNSUPPORTED_FEATURE!>,<!>/**/> foo11() {}

fun <T1,
        T2<!UNSUPPORTED_FEATURE!>,<!>
        > T2?.foo2() {}

val <T<!UNSUPPORTED_FEATURE!>,<!>> T.bar1 get() = null

var <
        T4<!UNSUPPORTED_FEATURE!>,<!>
        > T4?.bar2
    get() = null
    set(value) {

    }

typealias Foo3<T1, @Anno T2<!UNSUPPORTED_FEATURE!>,<!>> = List<T2>

typealias Foo4<T1, @Anno T2<!UNSUPPORTED_FEATURE!>,<!>/**/> = List<T2>

fun main() {
    fun <T<!UNSUPPORTED_FEATURE!>,<!>> foo10() {
        fun <T1,T2,T3<!UNSUPPORTED_FEATURE!>,<!>> foo10() {}
    }
}