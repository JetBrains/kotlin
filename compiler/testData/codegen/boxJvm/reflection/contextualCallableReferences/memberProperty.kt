// LANGUAGE: +ContextParameters +CallableReferencesToContextual
// TARGET_BACKEND: JVM
// WITH_REFLECT
// ISSUE: KT-86452

import kotlin.jvm.internal.Reflection
import kotlin.reflect.KCallable
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.KProperty
import kotlin.reflect.jvm.javaMethod
import kotlin.reflect.jvm.kotlinFunction
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class Cls(val tag: String) {
    var storage = ""

    context(c: String)
    val prop: String get() = tag + c

    context(c: String)
    var mutable: String
        get() = storage
        set(value) {
            storage = c + value
        }
}

context(c: String)
fun <T> describe(x: T): String = c + x

context(c: String)
val <T> List<T>.decorated: String
    get() = c + joinToString("")

fun box(): String {
    val pkg = Reflection.getOrCreateKotlinPackage(object {}::class.java.enclosingClass)

    context("ctx") {
        val prop = Cls::prop
        assertEquals(listOf(KParameter.Kind.INSTANCE), prop.parameters.map { it.kind })
        assertEquals("tctx", prop.get(Cls("t")))
        assertEquals("tctx", prop.call(Cls("t")))
        assertEquals("tctx", prop.getter.call(Cls("t")))

        val mutable = Cls::mutable
        val receiver = Cls("t")
        assertEquals(listOf(KParameter.Kind.INSTANCE), mutable.parameters.map { it.kind })
        assertEquals(listOf(KParameter.Kind.INSTANCE, KParameter.Kind.VALUE), mutable.setter.parameters.map { it.kind })
        mutable.setter.call(receiver, "V")
        assertEquals("ctxV", mutable.getter.call(receiver))
        mutable.set(receiver, "W")
        assertEquals("ctxW", mutable.get(receiver))

        val p1: KCallable<*> = Cls::prop
        val p2: KCallable<*> = Cls::prop
        assertEquals(p1, p2)
        assertEquals(p1.hashCode(), p2.hashCode())
        val f1: (Int) -> String = ::describe
        val f2: (Int) -> String = ::describe
        assertEquals(f1 as KCallable<*>, f2 as KCallable<*>)
        assertEquals(f1.hashCode(), f2.hashCode())

        val unboundDescribe = (f1 as KFunction<*>).javaMethod!!.kotlinFunction!!
        assertEquals(unboundDescribe.typeParameters, (f1 as KFunction<*>).typeParameters)
        assertEquals(listOf("T"), (f1 as KFunction<*>).typeParameters.map { it.name })

        val decorated = List<Int>::decorated
        assertEquals(listOf(KParameter.Kind.EXTENSION_RECEIVER), decorated.parameters.map { it.kind })
        assertEquals("ctx12", decorated.get(listOf(1, 2)))
        val unboundDecorated = pkg.members.single { it.name == "decorated" } as KProperty<*>
        assertEquals(unboundDecorated.typeParameters, decorated.typeParameters)
        assertEquals(listOf("T"), decorated.typeParameters.map { it.name })
    }

    val pa: KCallable<*> = context("a") { Cls::prop }
    val pb: KCallable<*> = context("b") { Cls::prop }
    assertNotEquals(pa, pb)
    val fa: (Int) -> String = context("a") { ::describe }
    val fb: (Int) -> String = context("b") { ::describe }
    assertNotEquals(fa as KCallable<*>, fb as KCallable<*>)

    return "OK"
}
