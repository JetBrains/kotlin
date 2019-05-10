// !LANGUAGE: +InlineClasses

// FILE: util.kt

inline class AsAny<T>(val x: Any?)
inline class AsInt(val x: Int)

// FILE: Reference.kt

fun <T, R> transform(a: AsAny<T>): AsAny<R> = a as AsAny<R>
fun <T, R> transformNullable(a: AsAny<T>?): AsAny<R> = a as AsAny<R> // unbox
fun <T, R> transformToNullable(a: AsAny<T>): AsAny<R>? = a as AsAny<R> // box
fun <T, R> transformToNullableTarget(a: AsAny<T>): AsAny<R>? = a as AsAny<R>? // box
fun <T, R> transformNullableToNullableTarget(a: AsAny<T>?): AsAny<R>? = a as AsAny<R>?

// FILE: Primitive.kt

fun transform(a: AsInt): AsInt = a as AsInt
fun transformNullable(a: AsInt?): AsInt = a as AsInt // unbox
fun transformToNullable(a: AsInt): AsInt? = a as AsInt // box
fun transformToNullableTarget(a: AsInt): AsInt? = a as AsInt? // box
fun transformNullableToNullableTarget(a: AsInt?): AsInt? = a as AsInt?

// @ReferenceKt.class:
// 2 INVOKESTATIC AsAny\.box
// 1 INVOKEVIRTUAL AsAny.unbox

// @PrimitiveKt.class:
// 2 INVOKESTATIC AsInt\.box
// 1 INVOKEVIRTUAL AsInt.unbox
