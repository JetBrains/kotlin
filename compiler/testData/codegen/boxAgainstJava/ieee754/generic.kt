// FILE: JavaClass.java

public class JavaClass<T> {

    private T minus0;

    private T plus0;

    JavaClass(T minus0, T plus0)
    {
        this.minus0 = minus0;
        this.plus0 = plus0;
    }

    public T minus0()
    {
        return minus0;
    }

    public T plus0()
    {
        return plus0;
    }

}

// FILE: b.kt

fun box(): String {
    val jClass = JavaClass<Double>(-0.0, 0.0)

    if (jClass.minus0() < jClass.plus0()) return "fail 2"
    if (jClass.minus0() != jClass.plus0()) return "fail 5"

    return "OK"
}