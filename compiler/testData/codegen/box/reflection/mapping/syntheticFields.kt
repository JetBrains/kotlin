// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS

// WITH_REFLECT

import kotlin.reflect.jvm.kotlin

enum class A {
    // There's a synthetic field "$VALUES" here
}

fun box(): String {
    for (field in A::class.java.getDeclaredFields()) {
        val prop = field.kotlin
        if (prop != null) return "Fail, property found: $prop"
    }

    return "OK"
}
