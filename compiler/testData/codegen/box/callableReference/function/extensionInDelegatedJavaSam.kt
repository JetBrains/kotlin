// TARGET_BACKEND: JVM
// FILE: Sam.java
public interface Sam  {
    String print(String a);
}
// FILE: 1.kt
fun String.foo(): String { return this }

class Derived(b: Sam) : Sam by b

fun box(): String {
    val a = Derived(Sam(String::foo))
    with(a){
        return print("OK")
    }
}