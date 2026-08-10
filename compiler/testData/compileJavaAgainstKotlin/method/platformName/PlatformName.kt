// FILE: PlatformName.java
package test;

public class PlatformName {
    public static void main(String[] args) {
        PlatformNameKt.bar();
    }
}

// FILE: PlatformName.kt
package test

@JvmName("bar")
fun foo() {}
