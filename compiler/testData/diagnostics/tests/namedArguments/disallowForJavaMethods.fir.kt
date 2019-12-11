// FILE: JavaSuperClass.java

public class JavaSuperClass {
    public void foo(int javaName) {}

    public void multipleParameters(int first, long second, String third) {}
}

// FILE: 1.kt

fun directInvocation() = JavaSuperClass().foo(javaName = 1)

open class KotlinSubClass : JavaSuperClass()

fun viaFakeOverride() = KotlinSubClass().foo(javaName = 2)

class KotlinSubSubClass : KotlinSubClass() {
    override fun foo(kotlinName: Int) {}
}

fun viaRealOverride() = KotlinSubSubClass().foo(kotlinName = 3)


fun unresolvedParameter() = JavaSuperClass().<!INAPPLICABLE_CANDIDATE!>foo<!>(nonexistentName = 4)


fun multipleParameters() = JavaSuperClass().multipleParameters(first = 1, second = 2L, third = "3")
