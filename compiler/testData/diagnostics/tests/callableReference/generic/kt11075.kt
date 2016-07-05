// KT-11075 NONE_APPLICABLE reported for callable reference to an overloaded generic function with expected type provided

class Test {
    fun <A> foo(x: A) = x
    fun <B> foo(x: List<B>) = x

    fun test0(): (Test, String) -> String = Test::foo

    fun <T> test1(): (Test, List<T>) -> List<T> = Test::foo
}
