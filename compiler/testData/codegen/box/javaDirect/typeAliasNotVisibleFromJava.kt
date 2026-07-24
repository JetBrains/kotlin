// TARGET_BACKEND: JVM_IR

// In `two/User.java` the bare name `Box` must resolve to the star-imported real class `one.Box`
// (JLS 7.5.2). A Kotlin `typealias two.Box` sits in `User`'s own package, so the higher-priority
// same-package step (JLS 6.4.1) probes `ClassId(two, Box)` first. That `ClassId` exists in FIR's
// symbol provider *as a type alias*, so before the fix java-direct wrongly accepted it and bound
// `Box` to `two.Impl` (which has no `value()`), breaking the build. PSI ignores the alias and
// resolves `one.Box`. `tryResolve` now filters out `FirTypeAliasSymbol`, matching PSI.

// FILE: one/Box.kt
package one

class Box {
    fun value() = "OK"
}

// FILE: two/aliases.kt
package two

class Impl

typealias Box = Impl

// FILE: two/User.java
package two;

import one.*;

public class User {
    // `Box` must bind to the star-imported `one.Box`, never to the same-package `typealias two.Box`.
    public Box get() {
        return new Box();
    }
}

// FILE: main.kt
import two.User

fun box(): String {
    return User().get().value()
}
