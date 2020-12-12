interface A<in T> {
    private fun f(): T {
        TODO()
    }
}

interface B<out T> {
    private fun f(): T {
        TODO()
    }
}