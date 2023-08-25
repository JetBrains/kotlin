// Test
interface A
interface B<T, R>

typealias OtherA = A
typealias OtherOtherA = OtherA
typealias OtherB<X, Y> = B<Y, X>

typealias TA = () -> Boolean

interface Test {
    fun foo(): A
    fun fooAliased(): OtherA

    fun bar(): B<A, B<A, String>>
    fun barAliased(): OtherB<OtherB<String, OtherA>, OtherOtherA>
}

class Foo(
    val p: TA?,
)
