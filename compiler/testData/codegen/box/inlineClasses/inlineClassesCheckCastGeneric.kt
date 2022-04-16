// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses, +GenericInlineClassParameter

OPTIONAL_JVM_INLINE_ANNOTATION
value class AsAny<T>(val x: T)

OPTIONAL_JVM_INLINE_ANNOTATION
value class AsAny2<T: Any>(val x: T?)

OPTIONAL_JVM_INLINE_ANNOTATION
value class AsInt<T: Int>(val x: T)

inline fun <reified T> Any?.checkcast(): T = this as T

object Reference {
    fun <T, R> transform(a: AsAny<T>): AsAny<R> = a as AsAny<R>
    fun <T, R> transformNullable(a: AsAny<T>?): AsAny<R> = a as AsAny<R>
    fun <T, R> transformToNullable(a: AsAny<T>): AsAny<R>? = a as AsAny<R>
    fun <T, R> transformToNullableTarget(a: AsAny<T>): AsAny<R>? = a as AsAny<R>?
    fun <T, R> transformNullableToNullableTarget(a: AsAny<T>?): AsAny<R>? = a as AsAny<R>?
}

object Reference2 {
    fun <T: Any, R: Any> transform(a: AsAny2<T>): AsAny2<R> = a as AsAny2<R>
    fun <T: Any, R: Any> transformNullable(a: AsAny2<T>?): AsAny2<R> = a as AsAny2<R>
    fun <T: Any, R: Any> transformToNullable(a: AsAny2<T>): AsAny2<R>? = a as AsAny2<R>
    fun <T: Any, R: Any> transformToNullableTarget(a: AsAny2<T>): AsAny2<R>? = a as AsAny2<R>?
    fun <T: Any, R: Any> transformNullableToNullableTarget(a: AsAny2<T>?): AsAny2<R>? = a as AsAny2<R>?
}

object Primitive {
    fun <T: Int> transform(a: AsInt<T>): AsInt<T> = a as AsInt<T>
    fun <T: Int> transformNullable(a: AsInt<T>?): AsInt<T> = a as AsInt<T>
    fun <T: Int> transformToNullable(a: AsInt<T>): AsInt<T>? = a as AsInt<T>
    fun <T: Int> transformToNullableTarget(a: AsInt<T>): AsInt<T>? = a as AsInt<T>?
    fun <T: Int> transformNullableToNullableTarget(a: AsInt<T>?): AsInt<T>? = a as AsInt<T>?
}

fun box(): String {
    val a = AsAny<Int>(42)
    val b1 = Reference.transform<Int, Number>(a)
    val b2 = Reference.transformNullable<Int, Number>(a)
    val b3 = Reference.transformToNullable<Int, Number>(a)
    val b4 = Reference.transformToNullableTarget<Int, Number>(a)
    val b5 = Reference.transformNullableToNullableTarget<Int, Number>(a)
    val b6 = Reference.transformNullableToNullableTarget<Int, Number>(null)

    val b7 = a.checkcast<AsAny<Number>>()
    if (b7.x != a.x) return "Fail 1"

    val a2 = AsAny2<Int>(42)
    val b21 = Reference2.transform<Int, Number>(a2)
    val b22 = Reference2.transformNullable<Int, Number>(a2)
    val b23 = Reference2.transformToNullable<Int, Number>(a2)
    val b24 = Reference2.transformToNullableTarget<Int, Number>(a2)
    val b25 = Reference2.transformNullableToNullableTarget<Int, Number>(a2)
    val b26 = Reference2.transformNullableToNullableTarget<Int, Number>(null)

    val b72 = a2.checkcast<AsAny2<Number>>()
    if (b72.x != a2.x) return "Fail 12"

    val c = AsInt(42)
    val d1 = Primitive.transform(c)
    val d2 = Primitive.transformNullable(c)
    val d3 = Primitive.transformToNullable(c)
    val d4 = Primitive.transformToNullableTarget(c)
    val d5 = Primitive.transformNullableToNullableTarget(c)
    val d6 = Primitive.transformNullableToNullableTarget<Int>(null)

    val d7 = c.checkcast<AsInt<Int>>()
    if (d7.x != c.x) return "Fail 2"

    return "OK"
}