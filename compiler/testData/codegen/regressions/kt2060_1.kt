import testing.ClassWithInternals

public class HelloServer() : ClassWithInternals() {
    public override fun start() {
        val test = foo() + someGetter //+ some
    }
}

fun box() : String {
    HelloServer().start()
    return "OK"
}