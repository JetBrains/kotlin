// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// SAM_CONVERSIONS: INDY
// LAMBDAS: INDY

// CHECK_BYTECODE_TEXT
// JVM_IR_TEMPLATES
// 1 java/lang/invoke/LambdaMetafactory

// ^ Since indy for SAM types with contravariant projections is disabled (see genericWithInProjection.kt), the bytecode in this test is
//   suboptimal. The lambda is generated via invokedynamic LambdaMetafactory and then wrapped into an instance of Cmp.
//   The optimal way would be to generate the Cmp instance via invokedynamic LambdaMetafactory directly.

fun interface Cmp<T> {
    fun compare(a: T, b: T): Int
}

fun <T> foo(comparator: Cmp<in T>, a: T, b: T) = comparator.compare(a, b)

fun bar(x: Int, y: Int) = foo({ a, b -> a - b}, x, y)

fun box(): String {
    val t = bar(42, 117)
    if (t != -75)
        return "Failed: t=$t"
    return "OK"
}
