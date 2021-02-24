// TARGET_BACKEND: JVM

// WITH_REFLECT

import kotlin.reflect.jvm.javaType
import kotlin.test.assertEquals

fun listOfStrings(): List<String> = null!!

class Foo<T>
class Bar
fun fooOfInvBar(): Foo<Bar> = null!!
fun fooOfInBar(): Foo<in Bar> = null!!
fun fooOfOutBar(): Foo<out Bar> = null!!

fun box(): String {
    assertEquals(String::class.java, ::listOfStrings.returnType.arguments.single().type!!.javaType)

    assertEquals(Bar::class.java, ::fooOfInvBar.returnType.arguments.single().type!!.javaType)
    assertEquals(Bar::class.java, ::fooOfInBar.returnType.arguments.single().type!!.javaType)
    assertEquals(Bar::class.java, ::fooOfOutBar.returnType.arguments.single().type!!.javaType)

    return "OK"
}
