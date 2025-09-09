// TARGET_BACKEND: JVM
// WITH_REFLECT

import kotlin.reflect.*
import kotlin.reflect.jvm.javaField
import kotlin.test.*

object Delegate {
    lateinit var property: KProperty<*>

    operator fun getValue(instance: Any?, kProperty: KProperty<*>): List<String?> {
        property = kProperty
        return emptyList()
    }

    operator fun setValue(instance: Any?, kProperty: KProperty<*>, value: List<String?>) {
        throw AssertionError()
    }
}

fun checkIsFinal(c: KCallable<*>) {
    assertTrue(c.isFinal)
    assertFalse(c.isOpen)
    assertFalse(c.isAbstract)
}

fun checkUnsupportedCall(block: () -> Unit) {
    try {
        block()
        throw AssertionError("Fail: reflective call of a local delegated property should fail because it's not supported")
    } catch (e: UnsupportedOperationException) {  /* ok */ }
}

fun check(expectedName: String, p: KProperty0<*>) {
    assertEquals(expectedName, p.name)
    assertEquals(emptyList<KParameter>(), p.parameters)
    assertEquals(emptyList<KTypeParameter>(), p.typeParameters)
    assertEquals(null, p.visibility) // "local" visibility is not representable with reflection API
    assertEquals("kotlin.collections.List<kotlin.String?>", p.returnType.toString())
    checkIsFinal(p)
    assertFalse(p.isLateinit)
    assertFalse(p.isConst)
    assertEquals(null, p.javaField)

    // TODO: support getDelegate for local delegated properties
    assertEquals(null, (p as KProperty0<*>).getDelegate())

    assertEquals(emptyList<KParameter>(), p.getter.parameters)
    assertEquals("kotlin.collections.List<kotlin.String?>", p.getter.returnType.toString())

    checkUnsupportedCall { p.call() }
    checkUnsupportedCall { p.callBy(emptyMap()) }

    val getter = p.getter
    checkIsFinal(getter)
    assertEquals(p, getter.property)
    assertEquals("<get-$expectedName>", getter.name)
    assertEquals(emptyList(), getter.parameters)
    assertEquals(p.returnType, getter.returnType)
    assertEquals(emptyList(), getter.typeParameters)
    assertEquals(null, getter.visibility)
    assertFalse(getter.isSuspend)
    assertFalse(getter.isInline)
    assertFalse(getter.isExternal)
    assertFalse(getter.isOperator)
    assertFalse(getter.isInfix)
    assertEquals(emptyList(), getter.annotations)
    checkUnsupportedCall { getter.call() }
    checkUnsupportedCall { getter.callBy(emptyMap()) }

    if (p is KMutableProperty0<*>) {
        val setter = p.setter
        val param = setter.parameters.single()

        checkIsFinal(setter)
        assertEquals(p, setter.property)
        assertEquals("<set-$expectedName>", setter.name)
        assertEquals("kotlin.Unit", setter.returnType.toString())
        assertEquals(emptyList(), setter.typeParameters)
        assertEquals(null, setter.visibility)
        assertFalse(setter.isSuspend)
        assertFalse(setter.isInline)
        assertFalse(setter.isExternal)
        assertFalse(setter.isOperator)
        assertFalse(setter.isInfix)
        assertEquals(emptyList(), setter.annotations)
        // Passing some argument here to mitigate the effect of KT-81377.
        checkUnsupportedCall { setter.call(listOf("")) }
        checkUnsupportedCall { setter.callBy(mapOf(param to listOf(""))) }

        assertEquals(0, param.index)
        assertEquals(null, param.name)
        assertEquals(KParameter.Kind.VALUE, param.kind)
        assertFalse(param.isOptional)
        assertFalse(param.isVararg)
        assertEquals(emptyList(), param.annotations)
    }
}

annotation class Anno

fun box(): String {
    @Anno
    val localVal by Delegate
    localVal
    val valProperty = Delegate.property as KProperty0<*>
    assertFalse(valProperty is KMutableProperty<*>)
    check("localVal", valProperty)

    @Anno
    var localVar by Delegate
    localVar
    var varProperty = Delegate.property as KProperty0<*>
    assertTrue(varProperty is KMutableProperty<*>)
    check("localVar", varProperty)

    return "OK"
}
