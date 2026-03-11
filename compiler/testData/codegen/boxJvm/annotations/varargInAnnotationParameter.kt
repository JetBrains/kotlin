// TARGET_BACKEND: JVM

// WITH_STDLIB

@Retention(AnnotationRetention.RUNTIME)
annotation class Ann(vararg val p: Int)

@Ann() class MyClass1
@Ann(1) class MyClass2
@Ann(1, 2) class MyClass3

@Ann(*intArrayOf()) class MyClass4
@Ann(*intArrayOf(1)) class MyClass5
@Ann(*intArrayOf(1, 2)) class MyClass6

@Ann(p = *intArrayOf()) class MyClass8
@Ann(p = *intArrayOf(1)) class MyClass9
@Ann(p = *intArrayOf(1, 2)) class MyClass10

fun box(): String {
    test(MyClass1::class.java, "")
    test(MyClass2::class.java, "1")
    test(MyClass3::class.java, "12")

    test(MyClass4::class.java, "")
    test(MyClass5::class.java, "1")
    test(MyClass6::class.java, "12")

    test(MyClass8::class.java, "")
    test(MyClass9::class.java, "1")
    test(MyClass10::class.java, "12")

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
