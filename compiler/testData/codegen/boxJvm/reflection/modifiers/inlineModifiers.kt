// TARGET_BACKEND: JVM
// WITH_REFLECT

import kotlin.reflect.*
import kotlin.reflect.full.*
import kotlin.test.*

class InlineSubject {
    inline fun plainInline(block: () -> Int): Int = block()
    inline fun crossinlineInline(crossinline block: () -> Int): () -> Int = { block() }
    inline fun noinlineInline(noinline block: () -> Int): () -> Int = block
    inline fun <reified T> reifiedInline(): String = T::class.simpleName ?: "?"
    inline fun <reified T, reified R : T> multiReifiedInline(x: T): R? = x as? R
    inline fun withBothModifiers(crossinline cross: () -> String, noinline noin: () -> Int): String = cross() + noin()
    fun notInline(x: Int): Int = x

    infix fun infixFun(other: Int): Int = other
    operator fun plus(other: InlineSubject): InlineSubject = this
    operator fun get(index: Int): String = index.toString()
    operator fun invoke(): String = "invoked"
}

fun box(): String {
    val fns = InlineSubject::class.memberFunctions.associateBy { it.name }

    // inline modifier
    assertTrue(fns["plainInline"]!!.isInline)
    assertTrue(fns["crossinlineInline"]!!.isInline)
    assertTrue(fns["noinlineInline"]!!.isInline)
    assertTrue(fns["reifiedInline"]!!.isInline)
    assertTrue(fns["multiReifiedInline"]!!.isInline)
    assertTrue(fns["withBothModifiers"]!!.isInline)
    assertFalse(fns["notInline"]!!.isInline)

    // infix modifier
    assertTrue(fns["infixFun"]!!.isInfix)
    assertFalse(fns["plainInline"]!!.isInfix)

    // operator modifier
    assertTrue(fns["plus"]!!.isOperator)
    assertTrue(fns["get"]!!.isOperator)
    assertTrue(fns["invoke"]!!.isOperator)
    assertFalse(fns["infixFun"]!!.isOperator)

    // Reified type parameters: isReified=true
    val reifiedFn = fns["reifiedInline"]!!
    assertEquals(1, reifiedFn.typeParameters.size)
    assertTrue(reifiedFn.typeParameters.single().isReified)

    val multiReified = fns["multiReifiedInline"]!!
    assertEquals(2, multiReified.typeParameters.size)
    assertTrue(multiReified.typeParameters.all { it.isReified })
    assertEquals("T", multiReified.typeParameters[0].name)
    assertEquals("R", multiReified.typeParameters[1].name)
    // R has upper bound T
    assertEquals(1, multiReified.typeParameters[1].upperBounds.size)

    // Non-reified inline function
    val plainFn = fns["plainInline"]!!
    if (plainFn.typeParameters.isNotEmpty()) {
        assertFalse(plainFn.typeParameters.all { it.isReified })
    }

    // crossinline / noinline: these are call-site modifiers that don't appear in KParameter
    // but the parameter kind is still VALUE
    val crossFn = fns["crossinlineInline"]!!
    val crossParam = crossFn.parameters.filter { it.kind == KParameter.Kind.VALUE }
    assertEquals(1, crossParam.size)
    assertEquals("block", crossParam[0].name)

    val noinlineFn = fns["noinlineInline"]!!
    val noinlineParam = noinlineFn.parameters.filter { it.kind == KParameter.Kind.VALUE }
    assertEquals(1, noinlineParam.size)
    assertEquals("block", noinlineParam[0].name)

    val bothFn = fns["withBothModifiers"]!!
    val bothParams = bothFn.parameters.filter { it.kind == KParameter.Kind.VALUE }
    assertEquals(2, bothParams.size)
    assertEquals("cross", bothParams[0].name)
    assertEquals("noin", bothParams[1].name)

    return "OK"
}
