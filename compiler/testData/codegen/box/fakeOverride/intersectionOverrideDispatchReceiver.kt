// TARGET_BACKEND: JVM
// FILE: box.kt
fun box(): String {
    val x: I = C()
    return if (x is Base<*>) x.foo.value as String else "FAIL"
}

class C : Base<String>(), I {
    override fun getFoo() = StringStub("OK")
}

abstract class Stub<T>(val value: T)

class StringStub(value: String) : Stub<String>(value)

// FILE: I.java
public interface I {
    public StringStub getFoo();
}

// FILE: Base.java
public abstract class Base<T> {
    public abstract Stub<T> getFoo();
}