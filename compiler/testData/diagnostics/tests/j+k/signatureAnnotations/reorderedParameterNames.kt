// FIR_IDENTICAL

// ANDROID_ANNOTATIONS
// FILE: A.java
import kotlin.annotations.jvm.internal.*;

public class A {
    public void connect(@ParameterName("host") String host, @ParameterName("port") int port) {
    }
}

// FILE: test.kt
fun main() {
    val test = A()
    test.connect("127.0.0.1", 8080)
    test.connect(host = "127.0.0.1", port = 8080)
    test.connect(port = 8080, host = "127.0.0.1")
}
