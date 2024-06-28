// SKIP_WHEN_OUT_OF_CONTENT_ROOT
// MEMBER_NAME_FILTER: something
// FILE: Derived.kt
class D<caret>erived : Base() {
    override fun getSomething() = "42"
}

// FILE: Base.java
public class Base {
    public String getSomething() {
        return "";
    }
}