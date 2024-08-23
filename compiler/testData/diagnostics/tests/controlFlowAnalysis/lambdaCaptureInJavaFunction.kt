// WITH_STDLIB
// FULL_JDK
// ISSUES: KT-70133

// FILE: A.java
import kotlin.jvm.functions.Function0;
import java.util.function.Function;

public class A {
    public void foo(Function0<Integer> a, String b){ }
    public static void bar(Function<Object, Object> a, String b){}
}

// FILE: test.kt

fun test1(){
    val x: String
    A().foo (
        { <!UNINITIALIZED_VARIABLE!>x<!>.length },
        if (true) { x = ""; "" } else { x = ""; "" }
    )
}

fun test2(){
    val x: String
    A.bar (
        { <!UNINITIALIZED_VARIABLE!>x<!>.length },
        if (true) { x = ""; "" } else { x = ""; "" }
    )
}

class B: A(){
    <!OVERRIDE_BY_INLINE!>override inline fun foo(a: () -> Int, b: String)<!> { }
}

fun test3(){
    val x: String
    B().foo (
        { <!UNINITIALIZED_VARIABLE!>x<!>.length },
        if (true) { x = ""; "" } else { x = ""; "" }
    )
}