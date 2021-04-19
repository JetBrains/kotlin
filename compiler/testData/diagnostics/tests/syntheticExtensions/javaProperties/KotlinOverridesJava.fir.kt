// FILE: KotlinFile.kt
abstract class KotlinClass : JavaClass(), KotlinInterface, JavaInterface {
    override fun getSomething1(): Int = 1
    override fun getSomething3(): String = ""
    override fun setSomething4(value: String) {}
    override fun getSomething5(): String = ""
}

interface KotlinInterface {
    public fun getSomething1(): Int
    public fun getSomething4(): String
}

fun foo(k: KotlinClass) {
    useInt(k.getSomething1())
    useInt(k.something1)

    useInt(k.getSomething2())
    useInt(k.something2)

    useString(k.getSomething3())
    useString(k.something3)

    k.setSomething4("")
    k.something4 += ""
    k.setSomething4(<!NULL_FOR_NONNULL_TYPE!>null<!>)
    k.something4 = <!NULL_FOR_NONNULL_TYPE!>null<!>

    useString(k.getSomething5())
    useString(k.something5)
    k.setSomething5(1)
    k.something5 = <!ASSIGNMENT_TYPE_MISMATCH!>1<!>
}

fun useInt(i: Int) {}
fun useString(i: String) {}

// FILE: JavaClass.java
public class JavaClass {
    public int getSomething1() { return 1; }
    public int getSomething2() { return 1; }
    public Object getSomething3() { return null; }
}

// FILE: JavaInterface.java
public interface JavaInterface {
    String getSomething4();
    void setSomething4(String value);

    Object getSomething5();
    void setSomething5(Object value);
}
