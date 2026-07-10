// LANGUAGE: +ContextParameters
// OPT_IN: kotlin.ExperimentalContextParameters
// TARGET_BACKEND: JVM
// WITH_REFLECT

// Parameter annotations must be mapped to the correct Kotlin parameters even when the JVM signature
// differs from the Kotlin one: no dispatch receiver for @JvmStatic (KT-88939), leading context parameters,
// an extension receiver, mangled signatures due to value class parameters, and a trailing Continuation.

package test

import kotlin.coroutines.*
import kotlin.reflect.*
import kotlin.reflect.full.callSuspend
import kotlin.test.assertEquals

@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class A(val v: String)

@JvmInline
value class Z(val x: Int)

object O {
    @JvmStatic
    context(@A("c") c: String)
    fun @receiver:A("r") Int.f(@A("x") x: Z, @A("y") y: Boolean = true): String = "$c$this${x.x}$y"

    @JvmStatic
    suspend fun g(@A("x") x: Int, @A("y") y: Z): Int = x + y.x

    @JvmStatic
    @setparam:A("v")
    var @receiver:A("r") Long.p: String
        get() = "get$this"
        set(value) {}
}

class C {
    companion object {
        @JvmStatic
        context(@A("c") c: String)
        fun @receiver:A("r") Int.f(@A("x") x: Z, @A("y") y: Boolean = true): String = "$c$this${x.x}$y"

        @JvmStatic
        suspend fun g(@A("x") x: Int, @A("y") y: Z): Int = x + y.x

        @JvmStatic
        @setparam:A("v")
        var @receiver:A("r") Long.p: String
            get() = "get$this"
            set(value) {}
    }
}

interface I {
    companion object {
        @JvmStatic
        context(@A("c") c: String)
        fun @receiver:A("r") Int.f(@A("x") x: Z, @A("y") y: Boolean = true): String = "$c$this${x.x}$y"

        @JvmStatic
        suspend fun g(@A("x") x: Int, @A("y") y: Z): Int = x + y.x

        @JvmStatic
        @setparam:A("v")
        var @receiver:A("r") Long.p: String
            get() = "get$this"
            set(value) {}
    }
}

private val KCallable<*>.parameterAnnotations: String
    get() = parameters.joinToString { p -> p.annotations.map { (it as A).v }.toString() }

private fun <T> runSuspend(block: suspend () -> T): T {
    var result: Result<T>? = null
    block.startCoroutine(Continuation(EmptyCoroutineContext) { result = it })
    return result!!.getOrThrow()
}

private fun check(owner: KClass<*>, instance: Any) {
    val f = owner.members.single { it.name == "f" } as KFunction<*>
    assertEquals("[], [c], [r], [x], [y]", f.parameterAnnotations)
    assertEquals(
        listOf(KParameter.Kind.INSTANCE, KParameter.Kind.CONTEXT, KParameter.Kind.EXTENSION_RECEIVER, KParameter.Kind.VALUE, KParameter.Kind.VALUE),
        f.parameters.map { it.kind },
    )
    // If annotations are shifted, arguments most likely are shifted too, so check that calls work as well.
    assertEquals("c12false", f.call(instance, "c", 1, Z(2), false))
    val (dispatch, context, receiver, x) = f.parameters
    assertEquals("c34true", f.callBy(mapOf(dispatch to instance, context to "c", receiver to 3, x to Z(4))))

    val g = owner.members.single { it.name == "g" } as KFunction<*>
    assertEquals("[], [x], [y]", g.parameterAnnotations)
    assertEquals(42, runSuspend { g.callSuspend(instance, 40, Z(2)) })

    val p = owner.members.single { it.name == "p" } as KMutableProperty<*>
    assertEquals("[], [r]", p.parameterAnnotations)
    assertEquals("[], [r]", p.getter.parameterAnnotations)
    assertEquals("[], [r], [v]", p.setter.parameterAnnotations)
    assertEquals("get5", p.getter.call(instance, 5L))
}

fun box(): String {
    check(O::class, O)
    check(C.Companion::class, C.Companion)
    check(I.Companion::class, I.Companion)
    return "OK"
}
