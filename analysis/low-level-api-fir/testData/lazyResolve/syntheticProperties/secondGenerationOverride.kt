// SKIP_WHEN_OUT_OF_CONTENT_ROOT
// MEMBER_NAME_FILTER: something
// FILE: Derived.kt
class Der<caret>ived : KotlinBase() {
    @Anno("second generation getSomething: ${getterProperty}")
    override fun getSomething(): String {
        "body: ${propertyFromBody}"
    }

    @Anno("second generation setSomething: ${setterProperty}")
    override fun setSomething(s: @Anno("setter type ${propertyFromSetter}") String) = println("str $s")
}

open class KotlinBase : Base() {
    @Anno("first generation getSomething: ${getterProperty}")
    override fun getSomething(): String {
        "body: ${propertyFromBody}"
    }

    @Anno("first generation setSomething: ${setterProperty}")
    override fun setSomething(s: @Anno("setter type ${propertyFromSetter}") String) {
        println(s)
    }
}

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.TYPE)
annotation class Anno(val s: String)

val getterProperty = 2.let { it + 1 }
val setterProperty = 2.let { it + 1 }
val propertyFromBody = "str" + 1.toString()
val propertyFromSetter = 1.toString() + "2"

// FILE: Base.java
public class Base {
    public String getSomething() {
        return "";
    }

    public void setSomething(String s) {

    }
}