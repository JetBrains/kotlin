// IGNORE_LIGHT_ANALYSIS
// IGNORE_BACKEND: JVM
// WITH_STDLIB
// MODULE: lib
// FILE: lib.kt

public class WhateverUseCase : UseCaseWithParameter<Result<Int>, Int> {
    override operator fun invoke(param: Result<Int>): Result<Int> {
        return param.onFailure {
            return if (it is NumberFormatException)
                Result.success(0)
            else
                Result.failure(it)
        }
    }
}

interface UseCaseWithParameter<P, R> {
    operator fun invoke(param: P) : Result<R>
}

// MODULE: main(lib)
// FILE: main.kt

fun box(): String {
    val useCase = WhateverUseCase()
    return if (useCase(Result.failure(NumberFormatException())) == Result.success(0)) "OK"
        else "Fail"
}
