// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS, NATIVE

// WITH_RUNTIME
// FILE: 1.kt

package thispackage

import otherpackage.*

fun box(): String {
    if (!localUse()) {
        return "local use failed"
    }
    if (!fromOtherPackage()) {
        return "use from other package failed"
    }
    return "OK"
}

fun localUse(): Boolean {
    val c = Runnable::class.java
    return (c.getName()!! == "java.lang.Runnable")
}

// FILE: 2.kt

package otherpackage

fun fromOtherPackage(): Boolean {
    val c = Runnable::class.java
    return (c.getName()!! == "java.lang.Runnable")
}
