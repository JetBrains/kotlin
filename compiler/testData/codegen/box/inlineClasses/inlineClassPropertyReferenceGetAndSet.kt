// !LANGUAGE: +InlineClasses
// WITH_RUNTIME

inline class Foo(val z: String)

var f = Foo("zzz")

fun box(): String {
    (::f).set(Foo("OK"))
    return (::f).get().z
}
// DONT_TARGET_EXACT_BACKEND: WASM
 //DONT_TARGET_WASM_REASON: PROPERTY_REFERENCES
