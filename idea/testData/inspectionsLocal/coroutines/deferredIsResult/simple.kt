// WITH_RUNTIME
// FIX: Add '.await()' to function result (breaks use-sites!)

package kotlinx.coroutines

fun <caret>myFunction(): Deferred<Int> {
    return GlobalScope.async { 42 }
}
