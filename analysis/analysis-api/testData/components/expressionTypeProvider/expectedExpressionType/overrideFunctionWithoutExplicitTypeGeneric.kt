interface Base<T> {
    fun test(): T
}

class Impl : Base<String> {
    override fun test() = <caret>foo
}