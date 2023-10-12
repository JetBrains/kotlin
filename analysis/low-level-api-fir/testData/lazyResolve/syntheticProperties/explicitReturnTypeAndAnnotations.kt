// SKIP_WHEN_OUT_OF_CONTENT_ROOT
// MEMBER_NAME_FILTER: something
// FILE: Derived.kt
class Der<caret>ived : Base() {
    @Anno("number: ${prop}")
    override fun getSomething(): @Anno("type: ${prop}") String {
        return "str".also(::println)
    }
}

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.TYPE)
annotation class Anno(val s: String)
val prop = 2.let { it + 1 }

// FILE: Base.java
public class Base {
    public String getSomething() {
        return "";
    }
}