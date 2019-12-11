// FILE: KotlinFile.kt
fun foo(o: JavaInterface2): Int {
    if (o is JavaClass) {
        o.something++
        return o.x + o.something2
    }
    return 0
}

// FILE: JavaClass.java
public abstract class JavaClass extends BaseClass implements JavaInterface {
    public int getSomething() { return 1; }
    public void setSomething(int value) { }
}

// FILE: BaseClass.java
public abstract class BaseClass implements JavaInterface {
}

// FILE: JavaInterface.java
public interface JavaInterface {
    int getX();
}

// FILE: JavaInterface2.java
public interface JavaInterface2 {
    int getSomething2();
}