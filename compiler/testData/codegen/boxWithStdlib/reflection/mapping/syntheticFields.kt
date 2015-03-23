import kotlin.reflect.jvm.kotlin

class A {
    // There's a synthetic "$kotlinClass" field here
}

fun box(): String {
    for (field in javaClass<A>().getDeclaredFields()) {
        val prop = field.kotlin
        if (prop != null) return "Fail, property found: $prop"
    }

    return "OK"
}
