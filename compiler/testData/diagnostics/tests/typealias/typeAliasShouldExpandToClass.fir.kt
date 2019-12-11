// !DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_PARAMETER -TOPLEVEL_TYPEALIASES_ONLY

typealias ToTypeParam1<T> = T
typealias ToTypeParam2<T> = ToTypeParam1<T>
typealias ToTypeParam3<T1, T2> = ToTypeParam2<T1>
typealias ToTypeParam4 = ToTypeParam1<Any>

typealias ToFun1 = () -> Unit
typealias ToFun2<T> = (T) -> Unit

class Outer {
    typealias ToTypeParam1<T> = T
    typealias ToTypeParam2<T> = ToTypeParam1<T>
    typealias ToTypeParam3<T1, T2> = ToTypeParam2<T1>
    typealias ToTypeParam4 = ToTypeParam1<Any>
}
