interface A {
    val result: Any
}
interface B : A {
    override val result: String
}

abstract class AImpl<out Self : Any>(override val result: Self) : A
class BImpl(result: String) : AImpl<String>(result), B

fun box(): String = (BImpl("OK") as B).result
