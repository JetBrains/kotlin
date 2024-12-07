// SKIP_WHEN_OUT_OF_CONTENT_ROOT
// MEMBER_NAME_FILTER: something
// FILE: Derived.kt
class D<caret>erived : Base() {
    override fun getSomething(): String = "42"
    override fun setSomething(s: String) {

    }
}

// FILE: Base.java
public class Base {
    public String getSomething() {
        return "";
    }

    public void setSomething(String s) {

    }
}
