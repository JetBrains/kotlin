class C {
    fun OK() {}

    companion object {
        val result = C::OK
    }
}

fun box(): String = C.result.name
