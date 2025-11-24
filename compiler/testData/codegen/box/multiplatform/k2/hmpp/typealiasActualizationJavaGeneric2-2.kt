// LANGUAGE: +MultiPlatformProjects
// TARGET_BACKEND: JVM

// MODULE: lib-common
expect class NonGen {
    fun foo(): String
}

expect class Gen<T> {
    fun put(value: T)
    fun get(): T
}

// MODULE: lib-platform()()(lib-common)
// FILE: JavaNonGen.java
public class JavaNonGen {
    public String foo() {
        return "NG";
    }
}

// FILE: JavaGen.java
public class JavaGen<T> {
    private T value;

    public void put(T v) { value = v; }
    public T get() { return value; }
}

// FILE: libPlatform.kt
actual typealias NonGen = JavaNonGen
actual typealias Gen<T> = JavaGen<T>

// MODULE: app-common(lib-common)
fun testCommon(ng: NonGen, g: Gen<String>): String {
    ng.foo()
    g.put("OK")
    return g.get()
}

// MODULE: app-platform(lib-platform)()(app-common)
fun box(): String {
    val ng = NonGen()
    val g = Gen<String>()
    val r = testCommon(ng, g)
    return if (r == "OK") "OK" else "FAIL"
}
