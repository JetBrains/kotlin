// !DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_PARAMETER -TOPLEVEL_TYPEALIASES_ONLY

class OuterClass<T1> {
    class NestedClass<T2>
    typealias NestedType<T> = NestedClass<T>
}

typealias ON1<T1, T2> = OuterClass<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><T1><!>.NestedClass<T2>
typealias ON2<T1, T2> = OuterClass<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><T1><!>.NestedType<T2>
typealias ON3<T2> = OuterClass.NestedType<T2>
