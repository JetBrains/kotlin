actual interface PClass
actual object PInterface
actual enum class PObject
actual annotation class PEnumClass
actual class PAnnotationClass

private actual object InternalObject
internal actual object PublicObject
public actual object PrivateObject

final actual class OpenClass
open actual class AbstractClass
abstract actual class FinalClass

actual class C1<A, Extra>
actual class C2<out B>
actual class C3<D, E : D?>

actual typealias C4<F> = C4Impl<F>
class C4Impl<F : Number>

actual abstract class ExtendsNumber : Any()
