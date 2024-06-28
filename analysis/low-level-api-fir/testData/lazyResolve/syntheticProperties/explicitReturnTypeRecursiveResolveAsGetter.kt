// SKIP_WHEN_OUT_OF_CONTENT_ROOT
// MEMBER_NAME_FILTER: something
// LAZY_MODE: Recursive
// IS_GETTER: true
// FILE: Derived.kt
class D<caret>erived : Base() {
    override fun getSomething(): String = "42"
}

// FILE: Base.java
public class Base {
    public String getSomething() {
        return "";
    }
}