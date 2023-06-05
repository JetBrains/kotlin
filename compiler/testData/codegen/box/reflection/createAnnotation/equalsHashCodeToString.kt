// TARGET_BACKEND: JVM
// WITH_REFLECT

package test

annotation class A
annotation class B(val s: String)

@A
@B("2")
fun javaReflectionAnnotationInstances() {}

fun box(): String {
    val createA = A::class.constructors.single()

    val a1 = createA.call()
    if (a1.toString() != "@test.A()") return "Fail: toString does not correspond to the documentation of java.lang.annotation.Annotation#toString: $a1"

    val a2 = createA.call()
    if (a1 === a2) return "Fail: instances created by the constructor should be different"
    if (a1 != a2) return "Fail: any instance of A should be equal to any other instance of A"
    if (a1.hashCode() != a2.hashCode()) return "Fail: hash codes of equal instances should be equal"
    if (a1.hashCode() != 0) return "Fail: hashCode does not correspond to the documentation of java.lang.annotation.Annotation#hashCode: ${a1.hashCode()}"

    val createB = B::class.constructors.single()
    val b1 = createB.call("1")
    if (b1.toString() != "@test.B(s=1)") return "Fail: toString does not correspond to the documentation of java.lang.annotation.Annotation#toString: $b1"
    if (b1 != b1) return "Fail: instance should be equal to itself"

    val b2 = createB.call("2")
    if (b1 == b2) return "Fail: instances with different data should not be equal"
    if (b1.hashCode() == b2.hashCode()) return "Fail: hash codes of different instances should very likely be also different"

    val a3 = ::javaReflectionAnnotationInstances.annotations.filterIsInstance<A>().single()
    if (a1 === a3) return "Fail: instance created by the constructor and the one obtained from Java reflection should be different"
    if (a1 != a3) return "Fail: instance created by the constructor should be equal to the one obtained from Java reflection"
    if (a3 != a1) return "Fail: instance obtained from Java reflection should be equal to the one created by the constructor"
    if (a1.hashCode() != a3.hashCode()) return "Fail: hash codes of equal instances should be equal"

    val b3 = ::javaReflectionAnnotationInstances.annotations.filterIsInstance<B>().single()
    if (b2 === b3) return "Fail: instance created by the constructor and the one obtained from Java reflection should be different"
    if (b2 != b3) return "Fail: instance created by the constructor should be equal to the one obtained from Java reflection"
    if (b3 != b2) return "Fail: instance obtained from Java reflection should be equal to the one created by the constructor"
    if (b2.hashCode() != b3.hashCode()) return "Fail: hash codes of equal instances should be equal"

    return "OK"
}
