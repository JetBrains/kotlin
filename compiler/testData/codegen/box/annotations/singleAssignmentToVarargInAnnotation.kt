// !LANGUAGE: -ProhibitAssigningSingleElementsToVarargsInNamedForm
// IGNORE_BACKEND: JS_IR
// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS, NATIVE

// WITH_RUNTIME

annotation class Ann(vararg val p: Int)

@Ann(p = 1) class MyClass

fun box(): String {
    test(MyClass::class.java, "1")
    return "OK"
}

fun test(klass: Class<*>, expected: String) {
    val ann = klass.getAnnotation(Ann::class.java)
    if (ann == null) throw AssertionError("fail: cannot find Ann on ${klass}")

    var result = ""
    for (i in ann.p) {
        result += i
    }

    if (result != expected) {
        throw AssertionError("fail: expected = ${expected}, actual = ${result}")
    }
}
