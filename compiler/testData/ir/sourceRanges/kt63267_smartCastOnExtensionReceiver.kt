class C {
    fun Any?.f() {
        if (this == null) return

        m()
        m()
        m()
        m()
        m()
    }

    fun Any.m() {}
}
