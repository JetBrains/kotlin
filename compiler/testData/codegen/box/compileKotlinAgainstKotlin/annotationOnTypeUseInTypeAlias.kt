// TARGET_BACKEND: JVM
// WITH_REFLECT

// JVM_ABI_K1_K2_DIFF: KT-63872

// MODULE: lib
// FILE: A.kt
@Target(AnnotationTarget.TYPE)
annotation class Anno(val value: String)

class Foo

typealias MyFoo = Foo
typealias MyMaybeFoo = Foo?

class C<T>(val t: T)

typealias MyCMyFoo = C<@Anno("OK") MyFoo?>
typealias MyCMaybeFoo = C<@Anno("OK") MyMaybeFoo>

// MODULE: main(lib)
// FILE: B.kt
fun testMyFoo(myc: MyCMyFoo) {}
fun testMyMaybeFoo(mycmyb: MyCMaybeFoo) {}

fun box(): String {
    testMyFoo(C(null))
    testMyMaybeFoo(C(null))

    for (fn in listOf(::testMyFoo, ::testMyMaybeFoo)) {
        val mycType = fn.parameters.single().type
        val argumentType = mycType.arguments.single().type!!
        if (!argumentType.isMarkedNullable)
            return "Fail on $fn: argument type should be seen as nullable"

        val annotations = argumentType.annotations
        if (annotations.toString() != "[@Anno(value=OK)]")
            return "Fail on $fn: $annotations"
    }

    return "OK"
}