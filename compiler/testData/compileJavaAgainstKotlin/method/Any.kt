// FILE: Any.java
package test;

class Any {
    {
        Object r = AnyKt.anyany(new Object(), null);
    }
}

// FILE: Any.kt
package test

// extra parameter is to preserve generic signature
fun anyany(a: kotlin.Any, ignore: java.util.List<String>) = a
