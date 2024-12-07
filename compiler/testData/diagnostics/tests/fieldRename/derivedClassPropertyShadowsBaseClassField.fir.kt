// RUN_PIPELINE_TILL: BACKEND
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
    val <!PROPERTY_HIDES_JAVA_FIELD!>regular<!> = "aa"

    val <!PROPERTY_HIDES_JAVA_FIELD!>withGetter<!> get() = "bb"

    lateinit var <!PROPERTY_HIDES_JAVA_FIELD!>lateInit<!>: String

    val <!PROPERTY_HIDES_JAVA_FIELD!>lazyProp<!> by lazy { "dd" }

    var <!PROPERTY_HIDES_JAVA_FIELD!>withSetter<!>: String = "ee"
        set(value) {
            println(value)
            field = value
        }

    open val <!PROPERTY_HIDES_JAVA_FIELD!>openProp<!> = "ff"
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
