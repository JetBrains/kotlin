// FILE: Derived.java
package test;

// It's not possible to inherit from Season (it's sealed in Kotlin)
public class Derived extends Season {

}

// FILE: Derived.kt
package test

public sealed class Season {
    class Warm: Season()
    class Cold: Season()
}
