// SKIP_WHEN_OUT_OF_CONTENT_ROOT
// MEMBER_NAME_FILTER: something
// IS_GETTER: true
// FILE: Derived.kt
class Der<caret>ived : Base() {
    @Anno("number: ${prop}")
    override fun getSomething() = "body: ${propertyFromBody}"
}

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.TYPE)
annotation class Anno(val s: String)
val prop = 2.let { it + 1 }
val propertyFromBody = "str" + 1.toString()

// FILE: Base.java
public class Base {
    public String getSomething() {
        return "";
    }
}