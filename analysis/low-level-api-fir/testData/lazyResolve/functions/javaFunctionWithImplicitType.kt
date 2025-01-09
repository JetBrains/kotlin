// SKIP_WHEN_OUT_OF_CONTENT_ROOT
// MEMBER_NAME_FILTER: defaultMethod
// FILE: main.kt
interface BaseKotlinInterface {
    fun defaultMethod()/*: String*/ = "";
}

class LeafKotlin<caret>Class : JavaExtension

// FILE: JavaExtension.java
public interface JavaExtension extends BaseKotlinInterface {
    @Override default String defaultMethod() { return ""; }
}
