import kotlin.reflect.typeOf
import kotlin.reflect.javaType

open class A

typealias Alias = Map<String, List<A?>?>

@JvmInline
value class V(val s: String)

@JvmInline
value class ValueBox<T>(val value: T)

@JvmInline
value class OutValueBox<out T>(val value: T)

@JvmInline
value class InValueBox<in T>(private val value: Any?)

inline fun <reified T> returnTypeOf(block: () -> T) = typeOf<T>()

fun <T> nonReifiedParamType(x: T) = typeOf<List<T>>().arguments.first().type!!

class G<T : A> {
    fun nonReifiedClassParamType() = typeOf<List<T>>().arguments.first().type!!
}

class Outer<T> {
    inner class Inner<U>
}

class WithNested {
    class Nested<T>
    object NestedObject
}

class WithCompanion {
    companion object
}

class WithNamedCompanion {
    companion object Named
}

class KotlinBox<T>

class KotlinPair<T, U>

class KotlinExtendsJavaBase : J.JavaBase<String>()

class KotlinImplementsJavaStringInterface : J.JavaInterface<String> {
    override fun value(): String = ""
}

interface KotlinInterfaceExtendsJavaInterface : J.JavaInterface<String>

class KotlinWithJavaBound<T : J.JavaInterface<String>> {
    fun nonReifiedJavaBoundParamType() = typeOf<List<T>>().arguments.first().type!!
}

class KotlinWithKotlinFunctionBound<T : () -> String> {
    fun nonReifiedKotlinFunctionBoundParamType() = typeOf<List<T>>().arguments.first().type!!
}

fun localTypes() = run {
    class Local
    class LocalGeneric<T>

    listOf(
        typeOf<Local>(),
        typeOf<List<Local?>>(),
        typeOf<LocalGeneric<A>>(),
        typeOf<LocalGeneric<J.JavaBox<String>>>(),
    )
}

val lightTypes = listOf(
    // simple
    typeOf<A>(),
    typeOf<Any>(),
    typeOf<Unit>(),
    typeOf<Int>(),
    typeOf<String>(),
    typeOf<List<String>>(),
    typeOf<MutableList<String>>(),
    typeOf<List<Nothing>>().arguments[0].type!!,
    // nullable
    typeOf<A?>(),
    typeOf<Any?>(),
    typeOf<Unit?>(),
    typeOf<List<String?>?>(),
    typeOf<MutableList<String?>?>(),
    typeOf<Int?>(),
    typeOf<String?>(),
    typeOf<List<Nothing?>>().arguments[0].type!!,
    // variance
    typeOf<List<out A>>(),
    typeOf<List<*>>(),
    typeOf<MutableList<in A>>(),
    typeOf<MutableList<out A>>(),
    typeOf<MutableList<*>>(),
    typeOf<Map<in String, out A?>>(),
    // arrays
    typeOf<IntArray>(),
    typeOf<Array<A>>(),
    typeOf<Array<A?>?>(),
    typeOf<Array<out A>>(),
    typeOf<Array<List<String?>?>>(),
    // nested classifiers
    typeOf<Alias>(),
    typeOf<G<A>>(),
    typeOf<Outer<String>.Inner<A?>>(),
    typeOf<WithNested.Nested<A?>>(),
    typeOf<WithNested.NestedObject>(),
    typeOf<WithCompanion.Companion>(),
    typeOf<WithNamedCompanion.Named>(),
    typeOf<V>(),
    typeOf<Array<V?>>(),
    // generic value classes
    typeOf<ValueBox<A>>(),
    typeOf<ValueBox<A?>>(),
    typeOf<ValueBox<J.JavaBox<String>>>(),
    typeOf<ValueBox<J.JavaInterface<String>>>(),
    typeOf<OutValueBox<J.JavaClassImplementsKotlinFunction>>(),
    typeOf<OutValueBox<A>>(),
    typeOf<InValueBox<KotlinInterfaceExtendsJavaInterface>>(),
    typeOf<InValueBox<J.JavaInterface<String>>>(),
    typeOf<ValueBox<out A>>(),
    typeOf<ValueBox<out J.JavaBox<String>>>(),
    typeOf<ValueBox<in A>>(),
    typeOf<ValueBox<in KotlinImplementsJavaStringInterface>>(),
    typeOf<Array<ValueBox<J.JavaBox<String>?>>>(),
    typeOf<List<OutValueBox<out J.JavaClassImplementsKotlinFunction>>>(),
    typeOf<Map<InValueBox<in J.JavaInterface<String>>, ValueBox<KotlinExtendsJavaBase>>>(),
    typeOf<J.JavaBox<ValueBox<A>>>(),
    // Java and Kotlin classifiers used across the language boundary
    typeOf<J.JavaBox<String>>(),
    typeOf<J.JavaBox<out Number>>(),
    typeOf<J.JavaInterface<String>>(),
    typeOf<KotlinExtendsJavaBase>(),
    typeOf<KotlinImplementsJavaStringInterface>(),
    // Kotlin generics with Java type arguments and Java generics with Kotlin type arguments
    typeOf<KotlinBox<J.JavaBox<String>>>(),
    typeOf<KotlinPair<J.JavaInterfaceExtendsKotlinFunction, J.JavaPair<String, Int>>>(),
    typeOf<J.JavaBox<KotlinImplementsJavaStringInterface>>(),
    typeOf<J.JavaPair<KotlinExtendsJavaBase, KotlinImplementsJavaStringInterface>>(),
    typeOf<J.JavaPair<KotlinBox<String>, KotlinPair<Int, A?>>>(),
    typeOf<J.JavaFunctionBoundBox<() -> String>>(),
    typeOf<J.JavaFunctionBoundBox<J.JavaInterfaceExtendsKotlinFunction>>(),
    typeOf<J.JavaInterfaceBoundBox<KotlinImplementsJavaStringInterface>>(),
    typeOf<J.JavaInterfaceBoundBox<KotlinInterfaceExtendsJavaInterface>>(),
    typeOf<J.JavaPair<ValueBox<out J.JavaClassImplementsKotlinFunction>, ValueBox<in KotlinImplementsJavaStringInterface>>>(),
    // function types
    typeOf<() -> A>(),
    typeOf<(String, Int?) -> A?>(),
    typeOf<A.(String?) -> List<Int?>>(),
    // flexible
    returnTypeOf { J.nullabilityFlexible() },
    returnTypeOf { J.mutabilityFlexible() },
    returnTypeOf { J.bothFlexible() },
    // Java interop
    returnTypeOf { J.primitiveArray() },
    returnTypeOf { J.stringArray() },
    returnTypeOf { J.nonNullStringArray() },
    returnTypeOf { J.listOfStringArrays() },
    returnTypeOf { J.wildcardOut() },
    returnTypeOf { J.wildcardIn() },
    returnTypeOf { J.unboundedWildcard() },
    returnTypeOf { J.nestedGeneric() },
    returnTypeOf { J.boxOfString() },
    returnTypeOf { J.boxOfWildcard() },
    returnTypeOf { J.rawList() },
    returnTypeOf { J.rawBox() },
    returnTypeOf { J.javaInterfaceExtendsKotlinFunction() },
    returnTypeOf { J.javaClassImplementsKotlinFunction() },
    returnTypeOf { J.boxOfJavaClassImplementsKotlinFunction() },
    // from non-reified type parameter
    nonReifiedParamType(1),
    G<A>().nonReifiedClassParamType(),
    KotlinWithJavaBound<KotlinImplementsJavaStringInterface>().nonReifiedJavaBoundParamType(),
    KotlinWithJavaBound<KotlinInterfaceExtendsJavaInterface>().nonReifiedJavaBoundParamType(),
    KotlinWithKotlinFunctionBound<() -> String>().nonReifiedKotlinFunctionBoundParamType(),
    // anonymous
    returnTypeOf { object : A() {} },
    returnTypeOf { object : J.JavaInterface<String> { override fun value(): String = "" } }
) + localTypes()

var ext: Any? = null

fun consume(value: Any?) {
    ext = value.hashCode()
}

@OptIn(ExperimentalStdlibApi::class)
fun main() {
    lightTypes.forEach {
        consume(it.classifier)
        consume(it.arguments)
        consume(it.javaType)
    }
}
