class C(x: Any?) {
    val s: String?
    init {
        s = x?.toString()
    }
}