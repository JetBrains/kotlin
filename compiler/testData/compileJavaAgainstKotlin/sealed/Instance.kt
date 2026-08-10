// FILE: Instance.java
package test;

public class Instance {
    // It is not possible to create Season instance (it's sealed in Kotlin)
    static Season create() {
        return new Season();
    }
}

// FILE: Instance.kt
package test

public sealed class Season {
    class Warm: Season()
    class Cold: Season()
}
