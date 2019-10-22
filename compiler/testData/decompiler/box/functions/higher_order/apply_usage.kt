fun box(): String {
    val t = ArrayList<Int>().apply {
        add(1)
        add(2)
        add(3)
        add(4)
    }
    if (t.size != 4) throw AssertionError()
    return "OK"
}