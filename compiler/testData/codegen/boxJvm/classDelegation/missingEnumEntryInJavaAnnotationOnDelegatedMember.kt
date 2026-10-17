// TARGET_BACKEND: JVM
// ISSUE: KT-90361
// ISSUE: KT-90363

// MODULE: lib
// FILE: arr/Restrict.kt
package arr

@Retention(AnnotationRetention.RUNTIME)
annotation class Restrict(
    val value: Array<Scope>,
    val scalar: Scope = Scope.MISSING,
) {
    enum class Scope { A, MISSING }
}

// FILE: arr/Greeter.java
package arr;

import arr.Restrict.Scope;

public interface Greeter {
    @Restrict(value = {Scope.A, Scope.MISSING})
    String array();

    @Restrict(value = {Scope.A}, scalar = Scope.MISSING)
    String scalar();
}

// MODULE: main(lib)
// FILE: arr/Restrict.kt
package arr

@Retention(AnnotationRetention.RUNTIME)
annotation class Restrict(
    val value: Array<Scope>,
    val scalar: Scope = Scope.A,
) {
    enum class Scope { A, B, C }
}

// FILE: main.kt
import arr.Greeter
import arr.Restrict

class GreeterImpl : Greeter {
    override fun array(): String = "array"
    override fun scalar(): String = "scalar"
}

class Delegator(delegate: Greeter) : Greeter by delegate

fun box(): String {
    val arrayAnnotation = Delegator::class.java.getDeclaredMethod("array")
        .getAnnotation(Restrict::class.java)
    if (!arrayAnnotation.value.contentEquals(arrayOf(Restrict.Scope.A))) return "FAIL: array value"
    if (arrayAnnotation.scalar != Restrict.Scope.A) return "FAIL: array default"

    val scalarAnnotation = Delegator::class.java.getDeclaredMethod("scalar")
        .getAnnotation(Restrict::class.java)
    if (!scalarAnnotation.value.contentEquals(arrayOf(Restrict.Scope.A))) return "FAIL: scalar value"
    if (scalarAnnotation.scalar != Restrict.Scope.A) return "FAIL: scalar default"

    return "OK"
}