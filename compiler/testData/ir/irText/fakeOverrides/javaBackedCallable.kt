// FIR_IDENTICAL
// TARGET_BACKEND: JKLIB
// FILE: BaseJava.java

public class BaseJava {
    private int field;
    public int getE() { return field; }
    /* package-private */ void setE(int value) { field = value; }

    public String getJavaString() { return "Java"; }
    public void doJavaAction() {}
}

// FILE: Kotlin1.kt
open class Kotlin1 : BaseJava()

// FILE: Kotlin2.kt
open class Kotlin2 : Kotlin1()

// FILE: Kotlin3.kt
class Kotlin3 : Kotlin2() {
    fun test() {
        val x = e
        e = x + 1
        getJavaString()
        doJavaAction()
    }
}
