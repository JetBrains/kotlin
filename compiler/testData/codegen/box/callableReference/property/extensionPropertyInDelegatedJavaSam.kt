// TARGET_BACKEND: JVM
// FILE: Sam.java
public interface Sam  {
    String print(String a);
}
// FILE: 1.kt
val String.foo : String
    get() = this

class Derived(b: Sam) : Sam by b

fun box(): String {
    val a = Derived(Sam(String::foo))
    with(a){
        return print("OK")
    }
}