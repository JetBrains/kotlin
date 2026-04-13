expect class PClass
expect interface PInterface
expect object PObject
expect enum class PEnumClass
expect annotation class PAnnotationClass

internal expect object InternalObject
public expect object PublicObject

open expect class OpenClass
abstract expect class AbstractClass
final expect class FinalClass

expect class C1<A>
expect class C2<B>
expect class C3<D, E : D>

expect class C4<F>


expect abstract class ExtendsNumber : Number

expect fun interface FunInterface {
    fun run()
}

expect fun interface FunInterface2 {
    fun run()
}
