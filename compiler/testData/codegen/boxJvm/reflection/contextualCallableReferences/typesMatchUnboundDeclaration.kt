// LANGUAGE: +ContextParameters +CallableReferencesToContextual
// TARGET_BACKEND: JVM
// WITH_REFLECT
// FULL_JDK
// ISSUE: KT-86452

import java.lang.reflect.TypeVariable
import kotlin.jvm.internal.Reflection
import kotlin.reflect.KCallable
import kotlin.reflect.KProperty
import kotlin.reflect.jvm.javaType
import kotlin.test.assertEquals

open class Base<T>(val v: T) {
    context(c: String)
    open val p: T get() = v
}

class Derived : Base<Int>(1)

context(c: String)
val <T> List<T>.head: T get() = first()

context(c: String)
val <T : CharSequence> T.len: Int get() = this.length + c.length

fun box(): String {
    val pkg = Reflection.getOrCreateKotlinPackage(object {}::class.java.enclosingClass)
    val unboundHead = pkg.members.single { it.name == "head" } as KProperty<*>
    val unboundLen = pkg.members.single { it.name == "len" } as KProperty<*>
    val unboundP = Derived::class.members.single { it.name == "p" } as KProperty<*>

    context("ctx") {
        val p = Derived::p
        assertEquals("kotlin.Int", p.returnType.toString())
        assertEquals(1, p.get(Derived()))
        assertEquals(unboundP.returnType, p.returnType)

        val head = List<Int>::head
        assertEquals("T", (head.returnType.javaType as TypeVariable<*>).name)
        assertEquals(unboundHead.typeParameters, head.typeParameters)
        assertEquals(unboundHead.typeParameters.hashCode(), head.typeParameters.hashCode())
        assertEquals(unboundHead.typeParameters.single().upperBounds, head.typeParameters.single().upperBounds)
        assertEquals(2, head.get(listOf(2, 3)))

        val len = String::len
        assertEquals(unboundLen.typeParameters, len.typeParameters)
        assertEquals(listOf("kotlin.CharSequence"), len.typeParameters.single().upperBounds.map { it.toString() })
        assertEquals(5, len.get("ab"))

        val boundHead = listOf(7)::head
        assertEquals(7, boundHead.get())
        assertEquals(unboundHead.typeParameters, boundHead.typeParameters)
        assertEquals(unboundHead.typeParameters.single().upperBounds, boundHead.typeParameters.single().upperBounds)
    }

    return "OK"
}
