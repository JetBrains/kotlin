// IGNORE_BACKEND_FIR: JVM_IR
class Binder() {
    lateinit var bindee: Container<*>

    fun bind(subject: Container<*>): String {
        bindee = subject
        return when(subject.containee) {
            is Foo -> bind(subject.containee)
            is Bar -> bind(subject.containee)
            else -> TODO()
        }
    }

    private fun bind(foo: Foo): String {
        return "binding $foo"
    }
    private fun bind(bar: Bar): String {
        return "binding $bar"
    }
}
class Container<out T>(val containee: T)
data class Foo(val x: Int = 0)
data class Bar(val y: Int = 0)

fun box(): String {
    val f = Container(Foo(1))
    val b = Container(Bar(2))

    val binder = Binder()

    if (binder.bind(f) != "binding Foo(x=1)") return "fail 1"
    if (binder.bind(b) != "binding Bar(y=2)") return "fail 2"

    return "OK"
}