import kotlin.reflect.*

open class A(private val p: Int)
class B : A(42)

fun box() =
        if (B::class.properties.isEmpty()) "OK"
        else "Fail: invisible fake overrides should not appear in KClass.properties"
