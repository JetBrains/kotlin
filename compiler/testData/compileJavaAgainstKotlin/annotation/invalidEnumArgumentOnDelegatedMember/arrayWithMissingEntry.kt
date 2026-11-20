// ISSUE: KT-90361

// FILE: neg/Marker.kt
package neg

@Retention(AnnotationRetention.RUNTIME)
annotation class Marker(vararg val value: Scope) {
    enum class Scope { A, B }
}

// FILE: neg/Greeter.java
package neg;

import neg.Marker.Scope;

public interface Greeter {
    @Marker({Scope.A, Scope.MISSING})
    String greet();
}

// FILE: neg/main.kt
package neg

class GreeterImpl : Greeter {
    override fun greet(): String = "greet"
}

class Delegator(delegate: Greeter) : Greeter by delegate
