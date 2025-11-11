// IGNORE_KLIB_RUNTIME_ERRORS_WITH_CUSTOM_SECOND_PHASE: 2.2.0
// ^^^ KT-79516 is fixed in 2.2.20-Beta1
interface Key<T, R>

interface ErrorTest {
    fun <T: Key<T, R>, R> get(key: T): R?
}

val errorTest = object : ErrorTest {
    override fun <T : Key<T, R>, R> get(key: T): R? = null
}

object IsValid : Key<IsValid, Boolean>

fun box(): String {
    val isValid = errorTest.get(IsValid)
    return if (isValid == null) "OK" else "FAIL: $isValid"
}
