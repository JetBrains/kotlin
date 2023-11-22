// WITH_STDLIB
// IGNORE_BACKEND: JS_IR

// KT-61141: absent enum fake_overrides: finalize, getDeclaringClass, clone
// IGNORE_BACKEND: NATIVE

enum class EE(val myName: String = this.toString().lowercase()) {
    ENTRY;
}
