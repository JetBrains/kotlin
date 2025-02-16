// RUN_PIPELINE_TILL: BACKEND
// TARGET_BACKEND: JVM
// LANGUAGE: +ContextParameters

// FILE: JavaInterface.java
public interface JavaInterface {
    void foo(Object a, Object b);
    Object getB(Object a);
    void bar(Object a, Object b, Object c);
}

// FILE: KotlinContextInterface.kt
interface KotlinContextInterface {
    context(a: String)
    fun foo(b: String)

    context(a: String)
    val b: String

    context(a: String)
    fun String.bar(b: String)
}

// FILE: test.kt
interface Intersection : KotlinContextInterface, JavaInterface

class IntersectionImpl : Intersection {
    context(a: String)
    override fun foo(b: String) { }

    context(a: String)
    override fun String.bar(b: String) { }

    context(a: String)
    override val b: String
        get() = ""

    override fun foo(a: Any?, b: Any?) { }

    override fun getB(a: Any?): Any? = ""

    override fun bar(a: Any?, b: Any?, c: Any?) { }
}

fun usage(a: Intersection) {
    with("context") {
        a.foo("value")
        a.b
        with(a) {
            "receiver".bar("value")
        }
    }
    a.foo("context", "value")
    a.getB("context")
    a.bar("context", "receiver", "value")
}