// LANGUAGE: +PartiallySpecifiedTypeArguments

sealed class MyResult<out T>{
    data class Success<T>(val value: T): MyResult<T>()
    data class Failure(val exception: Throwable): MyResult<Nothing>()
}

inline fun <reified E: Throwable, T> MyResult<T>.catch(result: (E) -> T) = "OK"

fun box(): String {
    val result: MyResult<Int> = MyResult.Success(1)
    return result.catch<IllegalStateException, _>{ 2 } // T is inferred into Int
}