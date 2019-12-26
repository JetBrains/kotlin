// !LANGUAGE: +NewInference
// FULL_JDK
// WITH_RUNTIME

/*
 * KOTLIN CODEGEN BOX NOT LINKED SPEC TEST (POSITIVE)
 *
 * SECTIONS: flexibility
 * NUMBER: 2
 * DESCRIPTION: check Nothing flexibillity
 * ISSUES: KT-35700
 */

// FILE: Test.java

public class Test<T> extends Base<T> {
    public Test (T arg) {
        super(arg);
    }
}


// FILE: KotlinClass.kt

open class Base<out T>(val prop: T)

class Inheritor <T : Any> {
    companion object {
        fun <T> default(): Base<T> = Test(null)
    }
}

fun box() : String{
    val v: Base<String> = Inheritor.default()
    try {
        println(v.prop.length)
    }catch (e:  Exception ){
        return "OK"
    }
    return "NOK"

}