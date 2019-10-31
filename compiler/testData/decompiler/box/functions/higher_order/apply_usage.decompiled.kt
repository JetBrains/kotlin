fun box() : String  {
    val t : ArrayList<Int> = java.util.ArrayList().apply {
        this.add(1)
        this.add(2)
        this.add(3)
        this.add(4)
    }
    if (t.size != 4) {
        throw java.lang.AssertionError()
    }
    return "OK"
}
