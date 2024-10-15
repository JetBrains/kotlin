// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
// RENDER_DIAGNOSTICS_FULL_TEXT

// FILE: JavaBase.java
import java.util.List;

public class JavaBase {
    public void test1(String a) {}

    public List<Integer> test2(List<Integer> a) { return null; }

    public String[] test3(String[] a) { return null; }

    public void test4(String... indices){}

    public int test5(int a){ return 1;}

    public Integer test6(Integer a){ return  1;}

    public Object test7(Object a){ return  1;}

    public void test8(JavaBase this){}
}

// FILE: test.kt
import JavaBase

class JavaBaseImpl: JavaBase() {
    <!NOTHING_TO_OVERRIDE!>override<!> fun test1(a: Int) {}
    <!NOTHING_TO_OVERRIDE!>override<!> fun test2(a: List<Double>): List<Double> { return null!! }
    <!NOTHING_TO_OVERRIDE!>override<!> fun test3(a: Array<CharSequence>): Array<CharSequence> { return null!! }
    <!NOTHING_TO_OVERRIDE!>override<!> fun test4(indices: String) {}
    <!NOTHING_TO_OVERRIDE!>override<!> fun test5(a: Number): Number {return null!!}
    <!NOTHING_TO_OVERRIDE!>override<!> fun test6(a: String): Int { return 1 }
    <!NOTHING_TO_OVERRIDE!>override<!> fun test7(a: Nothing): Any { return 1 }
    <!NOTHING_TO_OVERRIDE!>override<!> fun test8(a: JavaBase) {}
}