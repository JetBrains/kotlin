// FILE: KotlinFile.kt
fun foo(javaClass: JavaClass) {
    <!VAL_REASSIGNMENT!>javaClass.something1<!>++
    <!VAL_REASSIGNMENT!>javaClass.something2<!>++
    <!VAL_REASSIGNMENT!>javaClass.something3<!>++
    <!VAL_REASSIGNMENT!>javaClass.something4<!>++
    <!VAL_REASSIGNMENT!>javaClass.something5<!> = null
}

// FILE: JavaClass.java
public class JavaClass {
    public int getSomething1() { return 1; }
    public void setSomething1(int value, char c) { }

    public int getSomething2() { return 1; }
    public void setSomething2(String value) { }

    public int getSomething3() { return 1; }
    public <T> void setSomething3(int value) { return value; }

    public int getSomething4() { return 1; }
    public static void setSomething4(int value) { }

    public int[] getSomething5() { return null; }
    public void setSomething5(int... value) { }
}