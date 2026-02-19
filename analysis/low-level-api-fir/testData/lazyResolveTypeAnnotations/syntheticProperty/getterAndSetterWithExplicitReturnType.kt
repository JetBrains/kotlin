// SKIP_WHEN_OUT_OF_CONTENT_ROOT
// MEMBER_NAME_FILTER: something
// FILE: Derived.kt
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.TYPE)
annotation class Anno(val position: String)

const val prop = "str"

class Der<caret>ived : Base() {
    @Anno("getSomething: $prop")
    override fun getSomething(): @Anno("getter return type $prop") List<@Anno("getter nested return type $prop") Int> {}

    @Anno("getSomething: $prop")
    override fun setSomething(l: @Anno("setter type $prop") List<@Anno("setter nested type $prop") Int>) {
    }
}

// FILE: Base.java
public class Base {
    public List<Integer> getSomething() {
        return "";
    }

    public void setSomething(List<Integer> l) {

    }
}