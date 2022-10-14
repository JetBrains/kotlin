// WITH_STDLIB
// FILE: Base.java

public class Base {
    public String regular = "a";

    public String withGetter = "b";

    public String lateInit = "c";

    public String lazyProp = "d";

    public String withSetter = "e";

    public String openProp = "f";
}

// FILE: test.kt

open class Derived : Base() {
    val regular = "aa"

    val withGetter get() = "bb"

    lateinit var lateInit: String

    val lazyProp by lazy { "dd" }

    var withSetter: String = "ee"
        set(value) {
            println(value)
            field = value
        }

    open val openProp = "ff"
}

fun test(d: Derived) {
    d.regular
    d.withGetter
    d.lateInit
    d.lazyProp
    d.withSetter = ""
    d.openProp

    d::withGetter
    Derived::withGetter
}
