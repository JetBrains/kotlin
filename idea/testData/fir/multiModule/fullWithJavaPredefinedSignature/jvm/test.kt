interface UseIterable : MyIterable<String> {
    fun test() {
        val it = iterator()
        val split = spliterator()
    }
}
