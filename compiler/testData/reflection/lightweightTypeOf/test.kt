import kotlin.reflect.typeOf
import kotlin.reflect.javaType

open class A

inline fun <reified T> returnTypeOf(block: () -> T) = typeOf<T>()

fun <T> nonReifiedParamType(x: T) = typeOf<List<T>>().arguments.first().type!!

class G<T : A> {
    fun nonReifiedClassParamType() = typeOf<List<T>>().arguments.first().type!!
}

val lightTypes = listOf(
    // simple
    typeOf<A>(),
    typeOf<Any>(),
    typeOf<List<Nothing>>().arguments[0].type!!,
    typeOf<Unit>(),
    typeOf<List<String>>(),
    typeOf<MutableList<String>>(),
    typeOf<Int>(),
    typeOf<String>(),
    // nullable
    typeOf<A?>(),
    typeOf<Any>(),
    typeOf<List<Nothing?>>().arguments[0].type!!,
    typeOf<Unit?>(),
    typeOf<List<String?>?>(),
    typeOf<MutableList<String?>?>(),
    typeOf<Int?>(),
    typeOf<String?>(),
    // variance
    typeOf<List<out A>>(),
    typeOf<List<*>>(),
    typeOf<MutableList<in A>>(),
    typeOf<MutableList<out A>>(),
    typeOf<MutableList<*>>(),
    // flexible
    returnTypeOf { J.nullabilityFlexible() },
    returnTypeOf { J.mutabilityFlexible() },
    returnTypeOf { J.bothFlexible() },
    // from non-reified type parameter
    nonReifiedParamType(1),
    G<A>().nonReifiedClassParamType()
)

fun consume(value: Any?) {}

@OptIn(ExperimentalStdlibApi::class)
fun main() {
    lightTypes.forEach {
        consume(it.classifier)
        consume(it.arguments)
        consume(it.javaType)
    }
}