open class Base {
    companion object
}
interface IFoo
open class CG<T>
interface IG<T>

@Deprecated("Obsolete")
typealias Obsolete = Base

@Deprecated("Obsolete")
typealias IObsolete = IFoo

fun test1(x: Obsolete) = x
fun test1a(x: List<Obsolete>) = x

val test2 = Obsolete()

val test3 = Obsolete

class Test4: Obsolete()
class Test4a: IObsolete
class Test4b: IG<Obsolete>
class Test4c: CG<Obsolete>()