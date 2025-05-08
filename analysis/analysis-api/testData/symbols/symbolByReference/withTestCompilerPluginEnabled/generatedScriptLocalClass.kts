// WITH_FIR_TEST_COMPILER_PLUGIN
interface DataFrame<out T>

annotation class Refine

@Refine
fun <T, R> DataFrame<T>.add(columnName: String, expression: () -> R): DataFrame<Any?> = TODO()

fun local(df: DataFrame<*>) {
    val df1 = df.add("column") { 1 }
    df1.co<caret>lumn
}
