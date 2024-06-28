// FIR_IDENTICAL
// FILE: JavaSuperClass.java

public class JavaSuperClass {
    public void foo(int javaName) {}

    public void multipleParameters(int first, long second, String third) {}
}

// FILE: 1.kt

fun directInvocation() = JavaSuperClass().foo(<!NAMED_ARGUMENTS_NOT_ALLOWED!>javaName<!> = 1)

open class KotlinSubClass : JavaSuperClass()

fun viaFakeOverride() = KotlinSubClass().foo(<!NAMED_ARGUMENTS_NOT_ALLOWED!>javaName<!> = 2)

class KotlinSubSubClass : KotlinSubClass() {
    override fun foo(kotlinName: Int) {}
}

fun viaRealOverride() = KotlinSubSubClass().foo(kotlinName = 3)


fun unresolvedParameter() = JavaSuperClass().foo(<!NO_VALUE_FOR_PARAMETER!><!NAMED_ARGUMENTS_NOT_ALLOWED, NAMED_PARAMETER_NOT_FOUND!>nonexistentName<!> = 4)<!>


fun multipleParameters() = JavaSuperClass().multipleParameters(<!NAMED_ARGUMENTS_NOT_ALLOWED!>first<!> = 1, <!NAMED_ARGUMENTS_NOT_ALLOWED!>second<!> = 2L, <!NAMED_ARGUMENTS_NOT_ALLOWED!>third<!> = "3")
