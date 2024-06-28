// SKIP_WHEN_OUT_OF_CONTENT_ROOT
// MEMBER_NAME_FILTER: something
// FILE: Derived.kt
@Target(AnnotationTarget.TYPE, AnnotationTarget.FUNCTION)
annotation class Anno(val position: String)

const val prop = "str"

fun expectType(): @Anno("return type $prop") List<@Anno("nested return type $prop") Int> = 42

class D<caret>erived : Base() {
    @Anno("getSomething $prop")
    override fun getSomething() = expectType()
}

// FILE: Base.java

public class Base {
    public List<Integer> getSomething() {
        return "";
    }

}
