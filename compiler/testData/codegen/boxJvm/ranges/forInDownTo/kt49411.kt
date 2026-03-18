// TARGET_BACKEND: JVM
// WITH_STDLIB

abstract class Foo<T>(@JvmField val foo: T)

class Bar(foo: Int) : Foo<Int>(foo)

fun box(): String {
    var s1 = ""
    for (i in Bar(3).foo.downTo(1)) {
        s1 = s1 + i
    }
    if (s1 != "321") return "Failed: s1='$s1'"

    var s2 = ""
    for (i in 3.downTo(Bar(1).foo)) {
        s2 = s2 + i
    }
    if (s2 != "321") return "Failed: s2='$s2'"

    return "OK"
}
