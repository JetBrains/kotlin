// FILE: KotlinFile.kt
fun foo(javaClass: JavaClass) {
    javaClass.<!DEPRECATION!>something1<!>

    javaClass.<!DEPRECATION!>something2<!>
    javaClass.something2 = 1
    javaClass.<!DEPRECATION!>something2<!>++

    javaClass.something3
    javaClass.<!DEPRECATION!>something3<!> = 1
    javaClass.<!DEPRECATION!>something3<!>++

    javaClass.<!DEPRECATION!>something4<!>
    javaClass.<!DEPRECATION!>something4<!> = 1
    javaClass.<!DEPRECATION, DEPRECATION!>something4<!>++

    javaClass.<!DEPRECATION!>something5<!>
    javaClass.<!DEPRECATION!>something5<!> = 1
    javaClass.<!DEPRECATION, DEPRECATION!>something5<!>++
}

// FILE: JavaClass.java
public class JavaClass {
    @Deprecated
    public int getSomething1() { return 1; }

    @Deprecated
    public int getSomething2() { return 1; }
    public void setSomething2(int value) { }

    public int getSomething3() { return 1; }
    @Deprecated
    public void setSomething3(int value) { }

    @Deprecated
    public int getSomething4() { return 1; }
    @Deprecated
    public void setSomething4(int value) { }

    /**
     * @deprecated Bla-bla-bla
     */
    public int getSomething5() { return 1; }
    /**
     * @deprecated Ha-ha-ha
     */
    public void setSomething5(int value) { }
}
