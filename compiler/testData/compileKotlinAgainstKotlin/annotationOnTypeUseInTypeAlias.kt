// TARGET_BACKEND: JVM
// WITH_REFLECT
// FILE: A.kt
@Target(AnnotationTarget.TYPE)
annotation class Anno(val value: String)

class Foo

typealias MyFoo = Foo

class C<T>(val t: T)

typealias MyC = C<@Anno("OK") MyFoo?>

// FILE: B.kt
fun test(myc: MyC) {}

fun box(): String {
    test(C(null))

    val mycType = ::test.parameters.single().type
    val argumentType = mycType.arguments.single().type!!
    if (!argumentType.isMarkedNullable)
        return "Fail: argument type should be seen as nullable"

    val annotations = argumentType.annotations
    if (annotations.toString() != "[@Anno(value=OK)]")
        return "Fail: $annotations"

    return "OK"
}