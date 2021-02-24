// FULL_JDK
private typealias MyAlias = CharSequence

class A {
    private val foo = java.util.concurrent.ConcurrentHashMap<String, MyAlias>()

    private fun bar() {
        foo["dd"]?.baz()
    }

    private fun MyAlias.baz() {}
}
