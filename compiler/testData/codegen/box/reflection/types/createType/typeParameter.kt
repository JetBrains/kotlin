// TARGET_BACKEND: JVM

// WITH_REFLECT

import kotlin.reflect.full.createType
import kotlin.test.assertEquals

class Foo<T> {
    fun nonNull(): T = null!!
    fun nullable(): T? = null
}

fun box(): String {
    val tp = Foo::class.typeParameters.single()
    assertEquals(
            Foo::class.members.single { it.name == "nonNull" }.returnType,
            tp.createType()
    )
    assertEquals(
            Foo::class.members.single { it.name == "nullable" }.returnType,
            tp.createType(nullable = true)
    )

    assertEquals(tp.createType(), tp.createType())
    assertEquals(tp.createType(nullable = true), tp.createType(nullable = true))

    assertEquals("T", tp.createType().toString())
    assertEquals("T?", tp.createType(nullable = true).toString())

    return "OK"
}
