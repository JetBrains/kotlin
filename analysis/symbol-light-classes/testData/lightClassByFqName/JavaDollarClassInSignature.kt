// p.GreetingContainer
// LIBRARY_PLATFORMS: JVM

// FILE: p/Hello$World.java
package p;

public class Hello$World {
    public static class Nested {}
}

// FILE: p/GreetingContainer.kt
package p

class GreetingContainer(
    val hello: `Hello$World`,
    val nested: `Hello$World`.Nested,
) : `Hello$World`()
