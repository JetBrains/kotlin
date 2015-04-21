JavaAnn class MyClass
JavaAnn2 class MyClass2

fun box(): String {
    val ann = javaClass<MyClass>().getAnnotation(javaClass<JavaAnn>())
    if (ann == null) return "fail: cannot find Ann on MyClass}"
    if (ann.value() != "default") return "fail: annotation parameter i should be 'default', but was ${ann.value()}"

    val ann2 = javaClass<MyClass2>().getAnnotation(javaClass<JavaAnn2>())
    if (ann2 == null) return "fail: cannot find Ann on MyClass}"
    if (ann2.a() != 1) return "fail for a: expected = 1, but was ${ann2.a()}"
    if (ann2.b() != 1.toByte()) return "fail for b: expected = 1, but was ${ann2.b()}"
    if (ann2.c() != 1.toShort()) return "fail for c: expected = 1, but was ${ann2.c()}"
    if (ann2.d() != 1.0) return "fail for d: expected = 1, but was ${ann2.d()}"
    if (ann2.e() != 1F) return "fail for e: expected = 1, but was ${ann2.e()}"
    if (ann2.j() != 1L) return "fail for j: expected = 1, but was ${ann2.j()}"
    if (ann2.f() != "default") return "fail for f: expected = default, but was ${ann2.f()}"

    return "OK"
}
