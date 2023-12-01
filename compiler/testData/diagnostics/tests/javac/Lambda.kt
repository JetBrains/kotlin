// TARGET_BACKEND: JVM_IR
// WITH_STDLIB
// RENDER_DIAGNOSTICS_FULL_TEXT
// ISSUE: KT-62525

// MODULE: m1
// FILE: Request.kt

package com.request

sealed class Result<out Success, out Error> {
    class Success<out Success>(val value: Success) : Result<Success, Nothing>()

    class Error<out Error>(val error: Error) : Result<Nothing, Error>()

    inline fun <Mapped> mapError(transform: (Error) -> Mapped): Result<Success, Mapped> =
        when (this) {
            is Result.Success -> <!DEBUG_INFO_SMARTCAST!>this<!>
            is Result.Error -> Error(transform(<!DEBUG_INFO_IMPLICIT_RECEIVER_SMARTCAST!>error<!>))
        }
}

fun <T, U> request(success: T, error: U): Result<T, U> {
    if (1 + 1 + 2 == 4) {
        return Result.Success(success)
    } else {
        return Result.Error(error)
    }
}

// MODULE: m2
// FILE: Result.kt

package com.result

class A {}

sealed class B<T> {

    class HttpError<T>(val response: T) : B<T>() {}

    class Exception<T>(val exception: Throwable) : B<T>() {}
}

class C {}

class Owner<T> {
    inner class Nested<S> {
        inner class VeryNested<R, P>
    }
}

// MODULE: m3(m1, m2)
// FILE: Repo.kt

package com.repo

import com.request.Result
import com.request.request
import com.result.A
import com.result.B
import com.result.C
import com.result.Owner
import java.lang.RuntimeException

fun request_a(): Result<A, B<C>> {
    return request<A, B<C>>(A(), B.Exception(RuntimeException("Error")))
}

fun request_withNested(): Result<Owner<C>.Nested<A>, Owner<A>.Nested<C>.VeryNested<C, A>> {
    return request<Owner<C>.Nested<A>, Owner<A>.Nested<C>.VeryNested<C, A>>(Owner<C>().Nested<A>(), Owner<A>().Nested<C>().VeryNested<C, A>())
}

// MODULE: m4(m3, m1)
// FILE: Call.kt

package com.call

import com.repo.request_a
import com.repo.request_withNested

class Model {
    fun call() {
        request_a().mapError { 1 + 1 }
        request_a().mapError { it -> 1 + 1 }
        request_withNested().mapError { 1 + 1 }
    }
}

fun simple(b: com.<!UNRESOLVED_REFERENCE!>result<!>.<!DEBUG_INFO_MISSING_UNRESOLVED!>B<!><*>) {}
