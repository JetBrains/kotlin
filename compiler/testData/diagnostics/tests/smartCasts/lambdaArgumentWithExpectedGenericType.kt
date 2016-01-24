class My<T: Any>(val y: T?) {

    fun get(): T = run {
        val x = y
        if (x == null) throw Exception()
        <!DEBUG_INFO_SMARTCAST!>x<!>
    }
}