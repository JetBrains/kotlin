// ISSUE: KT-59851

sealed class Result4k<out T, out E>

data class Success<out T>(val value: T) : Result4k<T, Nothing>()
data class Failure<out E>(val reason: E) : Result4k<Nothing, E>()

inline fun <T, E> Result4k<T, E>.onFailure(block: (Failure<E>) -> Nothing): T = when (this) {
    is Success<T> -> value
    is Failure<E> -> block(this)
}

data class MyData(val a: Int, val b: List<String>)

fun interface MyDataWithoutAB {
    fun complete(mkB: (Int) -> String): MyData
}

fun doAThing(): Result4k<MyDataWithoutAB, Unit> {
    val list = listOf(1, 2, 3, 4)
    return Success(
        MyDataWithoutAB { mkB ->
            MyData(1, list.flatMap {
                List(it) { it }.map {
                    val int = failable().onFailure {
                        <!RETURN_NOT_ALLOWED!>return<!> it
                    }
                    mkB(int)
                }

            })
        }
    )
}

fun failable(): Result4k<Int, Unit> {
    return Success(1)
}
