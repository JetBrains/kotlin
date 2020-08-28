// !LANGUAGE: +Coroutines
// !DIAGNOSTICS: -USELESS_IS_CHECK
// SKIP_TXT

fun test() {
    suspend {} is SuspendFunction0<*>
    suspend {} is kotlin.coroutines.SuspendFunction0<*>
}
