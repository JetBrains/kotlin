trait First
trait Some<T: First> where T : Some<T>

val a: Some<*>? = null

class MClass(val p: String): First, Some<MClass>

fun box(): String {
    return MClass("OK").p
}
