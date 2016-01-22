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
