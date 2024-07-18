// ISSUE: KT-69567
// WITH_STDLIB
// SKIP_DESERIALIZED_IR_TEXT_DUMP

// filterIsInstance has following declaration
//   public inline fun <reified R> Iterable<*>.filterIsInstance(): List<@kotlin.internal.NoInfer R>
// where NoInfer has BINARY retention, and must appear in IR dumps for both declaration `val result` and symbol `filterIsInstance()`
//   @Target(AnnotationTarget.TYPE)
//   @Retention(AnnotationRetention.BINARY)
//   internal annotation class NoInfer

@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
fun filterInts(list: List<Any>): List<Int> {
    val result: List<@kotlin.internal.NoInfer Int> = list.filterIsInstance<Int>()
    return result
}
