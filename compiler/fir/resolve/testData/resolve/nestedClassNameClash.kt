fun <T> listOf(): List<T> = null!!

fun <T> materialize(): T = null!!

class Result

class Foo {
    fun test_0() {
        val result = materialize<Result>()
        saveResult(result)
    }

    fun test_1() {
        val results = listOf<Result>()
        saveResults(results)
    }

    fun test_2(results: List<Result>) {
        saveResults(results)
    }

    fun test_3(result: Result) {
        saveResult(result)
    }

    fun test_4() {
        val result = getResult()
        saveResult(result)
    }

    fun test_5() {
        val result = Result()
        saveResult(result)
    }

    private fun getResult(): Result = Result()
    private fun saveResults(results: List<Result>) {}
    private fun saveResult(result: Result) {}

    class Result
}