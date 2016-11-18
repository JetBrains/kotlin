// !DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_PARAMETER -TOPLEVEL_TYPEALIASES_ONLY

class Outer<T> {
    class Nested
    class GenericNested<TT>
    inner class Inner
    inner class GenericInner<TT>

    typealias NestedAlias = Nested
    typealias GenericNestedAlias<TT> = GenericNested<TT>
    typealias InnerAlias = Inner
    typealias GenericInnerAlias<TT> = GenericInner<TT>

    fun test1(x: NestedAlias) = x
    fun test2(x: GenericNestedAlias<Int>) = x
    fun <T> test3(x: GenericNestedAlias<T>) = x
    fun test4(x: InnerAlias) = x
    fun test5(x: GenericInnerAlias<Int>) = x
    fun <T> test6(x: GenericInnerAlias<T>) = x
}
fun test1(x: Outer<!TYPE_ARGUMENTS_FOR_OUTER_CLASS_WHEN_NESTED_REFERENCED!><Int><!>.NestedAlias) = <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>x<!>
fun <T> test2(x: Outer<!TYPE_ARGUMENTS_FOR_OUTER_CLASS_WHEN_NESTED_REFERENCED!><T><!>.NestedAlias) = <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>x<!>
fun test3(x: Outer.NestedAlias) = x
fun test4(x: Outer<!TYPE_ARGUMENTS_FOR_OUTER_CLASS_WHEN_NESTED_REFERENCED!><Int><!>.GenericNestedAlias<Int>) = <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>x<!>
fun <T> test5(x: Outer<!TYPE_ARGUMENTS_FOR_OUTER_CLASS_WHEN_NESTED_REFERENCED!><T><!>.GenericNestedAlias<Int>) = <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>x<!>
fun <T> test6(x: Outer<!TYPE_ARGUMENTS_FOR_OUTER_CLASS_WHEN_NESTED_REFERENCED!><Int><!>.GenericNestedAlias<T>) = <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>x<!>
fun test7(x: Outer.GenericNestedAlias<Int>) = x
fun <T> test8(x: Outer.GenericNestedAlias<T>) = x
fun test9(x: <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>Outer<!>.InnerAlias) = <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>x<!>
fun test10(x: Outer<Int>.InnerAlias) = x
fun <T> test11(x: Outer<T>.InnerAlias) = x
fun test12(x: <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>Outer<!>.GenericInnerAlias<Int>) = <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>x<!>
fun test13(x: Outer<Int>.GenericInnerAlias<Int>) = x
fun <T> test14(x: Outer<T>.GenericInnerAlias<Int>) = x
