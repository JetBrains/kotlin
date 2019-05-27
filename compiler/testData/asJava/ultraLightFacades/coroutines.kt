/** should load cls */

suspend fun doSomething(foo: String): Int {}

fun <T> async(block: suspend () -> T)