// ISSUE: KT-68747

// MODULE: m1
// FILE: result.kt
package failure

typealias FailureOr<F> = Result<F>

class Result<out R> (val value: Any?)

class Failure<out E>(val error: E) {}

fun <U> failure(): FailureOr<U> = Result(Failure(Unit))

fun <T> success(value: T): Result<T> = Result(value)


// MODULE: m2(m1)
// FILE: slow.kt

import failure.*

class Single<S : Any>(val initialValue: S? = null)

fun getLicense(key: String?): Single<FailureOr<String>> {
    return Single(
        key?.let { success(it) } ?: failure()
    )
}

fun <I, O> I.let(f: (I) -> O): O = f(this)

fun box(): String {
    if (getLicense(null).initialValue?.value !is Failure<*>) return "FAKE LICENSE"
    return "OK"
}
