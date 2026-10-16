// ISSUE: KT-90363

// FILE: neg/Marker.kt
package neg

@Retention(AnnotationRetention.RUNTIME)
annotation class Marker(val value: Scope) {
    enum class Scope { A, B }
}

// FILE: neg/Greeter.java
package neg;

public interface Greeter {
    @Marker(com.missing.Scope.VALUE)
    String greet();
}

// FILE: neg/main.kt
package neg

class GreeterImpl : Greeter {
    override fun greet(): String = "greet"
}

class Delegator(delegate: Greeter) : Greeter by delegate
