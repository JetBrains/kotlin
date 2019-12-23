// !LANGUAGE: +NewInference

class Query<out T : Any> private constructor(
    private val result: T?,
    private val error: Throwable?,
    val inProgress: Boolean
) {
    companion object {
        val inProgress = Query(null, null, true)
        fun forError(e: Throwable) = Query(null, e, false)
        fun <T : Any> forResult(result: T) = Query(result, null, false)
    }
}

class MutableLiveData<T> {
    var value: Query<Int> = null!!
}

fun main() {
    val liveData = MutableLiveData<Query<Int>>()
    liveData.value = Query.inProgress // Type mismatch: inferred type is Query<Any> but Query<Int> was expected
}
