// TARGET_BACKEND: JVM

// WITH_RUNTIME

annotation class A

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class B(val items: Array<A> = arrayOf(A()))

@B
class C

fun box(): String {
    val bClass = B::class.java
    val cClass = C::class.java

    val items = cClass.getAnnotation(bClass).items
    assert(items.size == 1) { "Expected: [A()], got ${items.asList()}" }
    assert(items[0] is A) { "Expected: [A()], got ${items.asList()}" }

    return "OK"
}
