// TARGET_BACKEND: JVM
// FILE: Sam.java
public interface Sam<T> {
    T foo();
}

// FILE: Connection.java
public interface Connection {
    <T> T accept(Sam<T> sam);
}

// FILE: box.kt
fun test(sam: Sam<*>, c: Connection): Any {
    return c.accept(sam)
}

fun box(): String {
    return test({ "OK" }, object: Connection {
        override fun <T> accept(sam: Sam<T>) = sam.foo()
    }) as String
}