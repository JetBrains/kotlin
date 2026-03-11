// TARGET_BACKEND: JVM
// WITH_STDLIB
// FULL_RUNTIME

// FILE: ResultHandler.java

import kotlin.Result;

@FunctionalInterface
public interface ResultHandler<T> {
    void onResult(Result<T> result);
}

// FILE: test.kt

fun doSmth(resultHandler: ResultHandler<Boolean>) {
    resultHandler.onResult(Result.success(true))
}

fun box(): String {
    var res = "FAIL"
    doSmth { result ->
        res = if (result.isSuccess) "OK" else "FAIL 1"
    }
    return res
}