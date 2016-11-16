abstract class Super {
    abstract fun bar()
}

class Foo : Super() {
    final override fun bar() {}
}
