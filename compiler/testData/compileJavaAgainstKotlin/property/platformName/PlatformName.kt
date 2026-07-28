// FILE: PlatformName.java
package test;

public class PlatformName {
    public static void main(String[] args) {
        int x = PlatformNameKt.vget();
        PlatformNameKt.vset(0);
    }
}

// FILE: PlatformName.kt
package test

var v: Int = 1
    @JvmName("vget")
    get
    @JvmName("vset")
    set
