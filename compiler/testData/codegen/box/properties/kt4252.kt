// KT-55828
// IGNORE_BACKEND_K2: NATIVE
class CallbackBlock {}

public class Foo
{
    companion object {
        private var bar = 0
    }

    init {
        ++bar
    }

    fun getBar(): Int = bar
}

fun box() : String {

    val foo = Foo()

    if (foo.getBar() != 1) return "Fail";

    return "OK"
}
