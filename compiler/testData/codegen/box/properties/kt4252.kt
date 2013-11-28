class CallbackBlock {}

public class Foo
{
    class object {
        private var bar = 0
    }

    {
        ++bar
    }

    fun getBar(): Int = bar
}

fun box() : String {

    val foo = Foo()

    if (foo.getBar() != 1) return "Fail";

    return "OK"
}