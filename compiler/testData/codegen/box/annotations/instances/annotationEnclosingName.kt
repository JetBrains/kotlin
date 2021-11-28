// TARGET_BACKEND: JVM_IR

// WITH_STDLIB
// !LANGUAGE: +InstantiationOfAnnotationClasses

// FILE: a.kt

package test

annotation class A1

annotation class A2

fun interface I {
    fun run(): A1
}

// FILE: test.kt

package test

class E {
    fun insideClass(): A1 = A1()
    fun insideLammbda(): A1 = run { A1() }
    fun insideSAM(): I = I { A1() }
}

class G {
    // test that we can reuse instance in different classes from same file
    fun insideClassAgain(): A1 = A1()
}

fun outsideClass(): A2 = A2()

fun test(instance: Any, parent: String, fqa: String) {
    val clz = instance.javaClass
    assert(clz.getName().startsWith(parent))
    assert(clz.getName().contains(fqa))
    assert(clz.getEnclosingMethod() == null)
    assert(clz.getEnclosingClass().getName() == parent)
    // SAM treated as anonymous because of Origin or something else, see ClassCodegen#IrClass.isAnonymousInnerClass
    // assert(clz.getDeclaringClass() == null)
}

fun box(): String {
    test(E().insideClass(), "test.E", "test_A1")
    test(E().insideLammbda(), "test.E", "test_A1")
    test(E().insideSAM().run(), "test.E", "test_A1")
    test(G().insideClassAgain(), "test.E", "test_A1")
    test(outsideClass(), "test.TestKt", "test_A2")
    return "OK"
}
