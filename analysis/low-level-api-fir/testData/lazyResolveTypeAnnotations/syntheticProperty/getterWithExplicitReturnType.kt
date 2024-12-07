// SKIP_WHEN_OUT_OF_CONTENT_ROOT
// MEMBER_NAME_FILTER: something
// FILE: Derived.kt
@Target(AnnotationTarget.TYPE, AnnotationTarget.FUNCTION)
annotation class Anno(val position: String)

const val prop = "str"

class D<caret>erived : Base() {
    @Anno("getSomething $prop")
    override fun getSomething(): @Anno("return type $prop") List<@Anno("nested return type $prop") Int> = 42
}

// FILE: Base.java

public class Base {
    public List<Integer> getSomething() {
        return "";
    }

}
