// !LANGUAGE: +InlineClasses
// WITH_RUNTIME
// IGNORE_BACKEND: JVM_IR
// IGNORE_BACKEND: JS_IR

inline class Foo(val arg: Int)
inline class AsAny(val arg: Any)

fun box(): String {
    val arr = arrayOf(Foo(1), Foo(2))
    var sum = 0
    for (el in arr) {
        sum += el.arg
    }

    if (sum != 3) return "Fail 1"

    sum = 0
    for (el in arrayOf(AsAny(42), AsAny(1))) {
        sum += el.arg as Int
    }

    if (sum != 43) return "Fail 2"

    return "OK"
}
