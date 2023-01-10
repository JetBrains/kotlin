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
    d.<!BASE_CLASS_FIELD_SHADOWS_DERIVED_CLASS_PROPERTY("Base; Derived")!>withGetter<!>
    d.<!BACKING_FIELD_ACCESSED_DUE_TO_PROPERTY_FIELD_CONFLICT("Base; Derived")!>lateInit<!>
    d.<!BASE_CLASS_FIELD_SHADOWS_DERIVED_CLASS_PROPERTY("Base; Derived")!>lazyProp<!>
    d.<!BACKING_FIELD_ACCESSED_DUE_TO_PROPERTY_FIELD_CONFLICT("Base; Derived")!>withSetter<!> = ""
    d.<!BASE_CLASS_FIELD_MAY_SHADOW_DERIVED_CLASS_PROPERTY("Base; Derived")!>openProp<!>

    d::<!BASE_CLASS_FIELD_SHADOWS_DERIVED_CLASS_PROPERTY!>withGetter<!>
    Derived::<!BASE_CLASS_FIELD_SHADOWS_DERIVED_CLASS_PROPERTY!>withGetter<!>
}
