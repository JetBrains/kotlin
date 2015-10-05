import A.foo
import A.bar
import A.baz

object A {
    var foo = "NotOk"
    var String.foo: String
        get() = "K"
        set(i) {}

    @JvmStatic
    val bar: String = "OK"

    @JvmStatic
    val String.baz: String
            get() = "OK"
}

fun box(): String {

    val static = (::bar).get()
    if (static != "OK") return static

    val staticExt = (String::baz).get("a")
    if (staticExt != "OK") return staticExt

    val nonExt = ::foo

    nonExt.set("O")
    val ext = String::foo
    ext.set("", "Whatever")
    return nonExt.get() + ext.get("")
}
