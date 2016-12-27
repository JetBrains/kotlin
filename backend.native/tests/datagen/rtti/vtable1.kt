abstract class Super {
    abstract fun bar()
}

class Foo : Super() {
    final override fun bar() {}
}

fun main(args: Array<String>) {
    // This test now checks that the source can be successfully compiled and linked;
    // TODO: check the contents of TypeInfo?
}