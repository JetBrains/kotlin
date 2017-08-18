import kotlin.reflect.*
import kotlin.reflect.jvm.*
import kotlin.reflect.full.*

class Test<T> {
    fun test() {
        Test::class.allSupertypes
        Test::class.createType(listOf(KTypeProjection.STAR))
        this::test.javaMethod
    }
}
