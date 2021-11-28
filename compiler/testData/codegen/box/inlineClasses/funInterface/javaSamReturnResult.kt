// TARGET_BACKEND: JVM
// WITH_STDLIB
// FULL_RUNTIME

// FILE: ResultHandler.java

import kotlin.Result;

@FunctionalInterface
public interface ResultHandler<T> {
    Result<T> onResult();
}

// FILE: test.kt

@Suppress("RESULT_CLASS_IN_RETURN_TYPE")
fun doSmth(resultHandler: ResultHandler<Boolean>): Result<Boolean> {
    return resultHandler.onResult()
}

fun box(): String {
    val res = doSmth { Result.success(true) }
    return if (res.isSuccess) "OK" else "FAIL"
}