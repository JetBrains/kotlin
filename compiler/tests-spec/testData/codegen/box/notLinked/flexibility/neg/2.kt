// FULL_JDK
// WITH_STDLIB

/*
 * KOTLIN CODEGEN BOX NOT LINKED SPEC TEST (NEGATIVE)
 *
 * SECTIONS: flexibility
 * NUMBER: 2
 * DESCRIPTION: check Nothing flexibillity
 * EXCEPTION: compiletime
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

class Inheritor<T : Any> {
    companion object {
        fun <T> default(): Base<T> = Test(null)
    }
}

fun box() {
    val v: Base<String> = Inheritor.default()
    println(v.prop.length)
}