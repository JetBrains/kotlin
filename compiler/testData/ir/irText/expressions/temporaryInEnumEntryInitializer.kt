// IGNORE_BACKEND: JS_IR

// KT-61141: absent enum fake_overrides: finalize, getDeclaringClass, clone
// IGNORE_BACKEND: NATIVE

val n: Any? = null

enum class En(val x: String?) {
    ENTRY(n?.toString())
}
