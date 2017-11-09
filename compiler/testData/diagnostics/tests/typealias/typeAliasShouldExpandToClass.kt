// !DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_PARAMETER -TOPLEVEL_TYPEALIASES_ONLY

typealias ToTypeParam1<T> = <!TYPEALIAS_SHOULD_EXPAND_TO_CLASS!>T<!>
typealias ToTypeParam2<T> = <!TYPEALIAS_SHOULD_EXPAND_TO_CLASS!>ToTypeParam1<T><!>
typealias ToTypeParam3<T1, <!UNUSED_TYPEALIAS_PARAMETER!>T2<!>> = <!TYPEALIAS_SHOULD_EXPAND_TO_CLASS!>ToTypeParam2<T1><!>
typealias ToTypeParam4 = ToTypeParam1<Any>

typealias ToFun1 = () -> Unit
typealias ToFun2<T> = (T) -> Unit

class Outer {
    typealias ToTypeParam1<T> = <!TYPEALIAS_SHOULD_EXPAND_TO_CLASS!>T<!>
    typealias ToTypeParam2<T> = <!TYPEALIAS_SHOULD_EXPAND_TO_CLASS!>ToTypeParam1<T><!>
    typealias ToTypeParam3<T1, <!UNUSED_TYPEALIAS_PARAMETER!>T2<!>> = <!TYPEALIAS_SHOULD_EXPAND_TO_CLASS!>ToTypeParam2<T1><!>
    typealias ToTypeParam4 = ToTypeParam1<Any>
}
