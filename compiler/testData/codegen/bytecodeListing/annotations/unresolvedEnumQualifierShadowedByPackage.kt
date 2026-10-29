// TARGET_BACKEND: JVM
// ISSUE: KT-90357
// DISABLE_JAVA_FACADE
// USE_PSI_JAVA_FACADE: false

// MODULE: decoy
// FILE: p/Outer/Inner.kt
package p.Outer

enum class Inner { X }

// MODULE: main(decoy)
// FILE: p/Marker.kt
package p

@Retention(AnnotationRetention.RUNTIME)
annotation class Marker(val value: Expected) {
    enum class Expected { VALUE }
}

class Outer

// FILE: p/Greeter.java
package p;

public interface Greeter {
    // javac resolves p.Outer as the type, so Inner must be nested in Outer, not a decoy package type.
    @Marker(p.Outer.Inner.X)
    String greet();
}

// FILE: main.kt
package p

class Delegator(delegate: Greeter) : Greeter by delegate