// !LANGUAGE: +InlineClasses
// WITH_RUNTIME

inline class Foo(val z: String)

var f = Foo("zzz")

fun box(): String {
    (::f).set(Foo("OK"))
    return (::f).get().z
}