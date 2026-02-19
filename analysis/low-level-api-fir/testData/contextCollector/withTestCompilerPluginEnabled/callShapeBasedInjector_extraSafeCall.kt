// WITH_FIR_TEST_COMPILER_PLUGIN

import kotlin.reflect.KClass

interface DataFrame<out T>

annotation class Refine

@Refine
fun <T, R> DataFrame<T>.add(columnName: String, expression: () -> R): DataFrame<Any?>? = TODO()

fun test_1(df: DataFrame<*>?) {
    val df1_toStringSafe = df?.add("column") { 1 }?.toString()

    <expr>df1_toStringSafe</expr>
}