// TARGET_BACKEND: JVM
// ISSUE: KT-90014

// FILE: test/Ann.kt
package test

annotation class Ann(val value: Scope) {
    enum class Scope {
        FIRST, VALUE
    }
}

// FILE: test/JavaInterface.java
package test;

import static test.Ann.Scope.VALUE;

public interface JavaInterface {
    @Ann(VALUE)
    String foo();
}

// FILE: test/Delegator.kt
package test

class Delegator(impl: JavaInterface) : JavaInterface by impl

fun box(): String {
    val scope = Delegator::class.java.getMethod("foo").getAnnotation(Ann::class.java)?.value
    if (scope != Ann.Scope.VALUE) return "Fail: $scope"
    return Delegator { "OK" }.foo()
}
