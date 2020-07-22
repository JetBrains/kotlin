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

    fun test1(x: <!OTHER_ERROR, OTHER_ERROR, OTHER_ERROR, OTHER_ERROR!>NestedAlias<!>) = x
    fun test2(x: <!OTHER_ERROR, OTHER_ERROR, OTHER_ERROR, OTHER_ERROR!>GenericNestedAlias<Int><!>) = x
    fun <T> test3(x: <!OTHER_ERROR, OTHER_ERROR, OTHER_ERROR, OTHER_ERROR!>GenericNestedAlias<T><!>) = x
    fun test4(x: <!OTHER_ERROR, OTHER_ERROR, OTHER_ERROR, OTHER_ERROR!>InnerAlias<!>) = x
    fun test5(x: <!OTHER_ERROR, OTHER_ERROR, OTHER_ERROR, OTHER_ERROR!>GenericInnerAlias<Int><!>) = x
    fun <T> test6(x: <!OTHER_ERROR, OTHER_ERROR, OTHER_ERROR, OTHER_ERROR!>GenericInnerAlias<T><!>) = x
}
fun test1(x: Outer<Int>.NestedAlias) = x
fun <T> test2(x: Outer<T>.NestedAlias) = x
fun test3(x: Outer.NestedAlias) = x
fun test4(x: Outer<Int>.GenericNestedAlias<Int>) = x
fun <T> test5(x: Outer<T>.GenericNestedAlias<Int>) = x
fun <T> test6(x: Outer<Int>.GenericNestedAlias<T>) = x
fun test7(x: Outer.GenericNestedAlias<Int>) = x
fun <T> test8(x: Outer.GenericNestedAlias<T>) = x
fun test9(x: Outer.InnerAlias) = x
fun test10(x: Outer<Int>.InnerAlias) = x
fun <T> test11(x: Outer<T>.InnerAlias) = x
fun test12(x: Outer.GenericInnerAlias<Int>) = x
fun test13(x: Outer<Int>.GenericInnerAlias<Int>) = x
fun <T> test14(x: Outer<T>.GenericInnerAlias<Int>) = x
