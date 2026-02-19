class MyList : ArrayList<Int>() {
    override val size<caret>: Int = 0
    override fun get(index: Int): Int = 0
}
