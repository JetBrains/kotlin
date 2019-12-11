// FILE: KotlinFile.kt
fun Any.foo(): Int {
    if (this is JavaClass) {
        something++
        return x
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
