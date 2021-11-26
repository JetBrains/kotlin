// WITH_STDLIB

/*
 * KOTLIN CODEGEN BOX SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-220
 * MAIN LINK: expressions, not-null-assertion-expression -> paragraph 2 -> sentence 2
 * PRIMARY LINKS: expressions, not-null-assertion-expression -> paragraph 2 -> sentence 1
 * NUMBER: 1
 * DESCRIPTION: For an expression e!!, if the type of e is nullable, a not-null assertion expression checks, whether the evaluation result of e is equal to null and, if it is, throws a runtime exception.
 */


// MODULE: libModule
// FILE: libModule/JavaClass.java
package libModule;

public class JavaClass {
    public static Boolean FALSE;
}


// MODULE: mainModule(libModule)
// FILE: KotlinClass.kt
package mainModule
import libModule.*


fun box(): String {
    try {
        val x = JavaClass.FALSE!!
    }catch (e: java.lang.NullPointerException){
        return "OK"
    }
    return "NOK"
}