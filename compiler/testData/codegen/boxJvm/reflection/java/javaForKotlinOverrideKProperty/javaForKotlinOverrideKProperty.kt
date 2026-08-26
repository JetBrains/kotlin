// TARGET_BACKEND: JVM
// WITH_REFLECT
// FILE: test/Annotations.java
package test;

import java.lang.annotation.*;

public interface Annotations {
    @Retention(RetentionPolicy.RUNTIME)
    @interface Property {}
    @Retention(RetentionPolicy.RUNTIME)
    @interface GetMethod {}
    @Retention(RetentionPolicy.RUNTIME)
    @interface SetMethod {}
}

// FILE: test/AX.java
package test;

public abstract class AX extends A {
    @Override
    @Annotations.GetMethod
    public AX getMemberVal() {
        return null;
    }

    private AX memberVarStorage;

    @Override
    @Annotations.GetMethod
    public AX getMemberVar() {
        return memberVarStorage;
    }

    @Override
    @Annotations.SetMethod
    public void setMemberVar(A value) {
        memberVarStorage = (AX) value;
    }
}

// FILE: test/A.kt
package test

import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.reflect.*
import kotlin.reflect.full.isSubtypeOf

abstract class A {
    @Annotations.Property
    abstract val memberVal: A?
    @Annotations.Property
    abstract var memberVar: A?
}

fun checkType(type: KType, flexible: Boolean) {
    assertEquals(AX::class, type.classifier)
    assertEquals(emptyList(), type.arguments)
    if (flexible) {
        assertFalse(type.isMarkedNullable)
        assertTrue(typeOf<AX?>().isSubtypeOf(type))
    } else {
        assertTrue(type.isMarkedNullable)
    }
}

private fun check(p: KProperty1<*, *>, isMutable: Boolean, name: String) {
    val instance = object : AX() {}

    assertEquals(name, p.name)

    val ip = p.parameters.single()
    assertEquals(null, ip.name)
    assertEquals(0, ip.index)
    assertEquals(KParameter.Kind.INSTANCE, ip.kind)
    assertEquals(AX::class, ip.type.classifier)
    assertFalse(ip.isOptional)
    assertFalse(ip.isVararg)

    assertEquals(emptyList(), p.typeParameters)
    checkType(p.returnType, flexible = false)
    assertEquals(KVisibility.PUBLIC, p.visibility)

    assertFalse(p.isFinal)
    assertTrue(p.isOpen)
    assertFalse(p.isAbstract)
    assertFalse(p.isSuspend)
    assertFalse(p.isLateinit)
    assertFalse(p.isConst)

    assertEquals("[]", p.annotations.toString().replace('$', '.'))

    p as KProperty1<Any?, *>

    assertEquals(null, p.getDelegate(instance))

    val getter = p.getter
    assertEquals("<get-$name>", getter.name)

    val gip = getter.parameters.single()
    assertEquals(null, gip.name)
    assertEquals(0, gip.index)
    assertEquals(KParameter.Kind.INSTANCE, gip.kind)
    assertEquals(AX::class, gip.type.classifier)

    assertEquals(emptyList(), getter.typeParameters)
    checkType(getter.returnType, flexible = false)
    assertEquals(KVisibility.PUBLIC, getter.visibility)

    assertFalse(getter.isFinal)
    assertTrue(getter.isOpen)
    assertFalse(getter.isAbstract)
    assertFalse(getter.isSuspend)
    assertFalse(getter.isInline)
    assertFalse(getter.isExternal)
    assertFalse(getter.isOperator)
    assertFalse(getter.isInfix)

    assertEquals("[@test.Annotations.GetMethod()]", getter.annotations.toString().replace('$', '.'))

    assertEquals(p, getter.property)
    assertEquals("getter of $p", getter.toString())

    getter as KProperty1.Getter<*, Any?>

    assertEquals(null, p.get(instance))
    assertEquals(null, p.invoke(instance))
    assertEquals(null, p.call(instance))
    assertEquals(null, p.callBy(mapOf(p.parameters.single() to instance)))
    assertEquals(null, getter(instance))
    assertEquals(null, getter.call(instance))
    assertEquals(null, getter.callBy(mapOf(getter.parameters.single() to instance)))

    if (isMutable) {
        p as KMutableProperty1<Any?, Any?>
        val setter = p.setter

        assertEquals("<set-$name>", setter.name)
        assertEquals(emptyList(), setter.typeParameters)
        assertEquals(typeOf<Unit>(), setter.returnType)
        assertEquals(KVisibility.PUBLIC, setter.visibility)

        assertFalse(setter.isFinal)
        assertTrue(setter.isOpen)
        assertFalse(setter.isAbstract)
        assertFalse(setter.isSuspend)
        assertFalse(setter.isInline)
        assertFalse(setter.isExternal)
        assertFalse(setter.isOperator)
        assertFalse(setter.isInfix)

        assertEquals("[@test.Annotations.SetMethod()]", setter.annotations.toString().replace('$', '.'))

        val sip = setter.parameters[0]
        assertEquals(null, sip.name)
        assertEquals(0, sip.index)
        assertEquals(KParameter.Kind.INSTANCE, sip.kind)
        assertEquals(AX::class, sip.type.classifier)

        val valueParam = setter.parameters[1]
        assertEquals(null, valueParam.name)
        val systemProperties = Class.forName("kotlin.reflect.jvm.internal.SystemPropertiesKt")
        if (systemProperties.getMethod("getUseK1Implementation").invoke(null) == true ||
            systemProperties.getMethod("getUseK1ImplementationForMembers").invoke(null) == true
        ) {
            // Setter parameter type is flexible in K1 implementation for some reason.
            checkType(valueParam.type, flexible = true)
        } else {
            checkType(valueParam.type, flexible = false)
        }
        assertEquals(1, valueParam.index)
        assertEquals(KParameter.Kind.VALUE, valueParam.kind)
        assertFalse(valueParam.isOptional)
        assertFalse(valueParam.isVararg)

        assertEquals(p, setter.property)
        assertEquals("setter of $p", setter.toString())

        val a = object : AX() {}
        val b = object : AX() {}
        p.set(instance, a)
        assertEquals(a, p.get(instance))
        setter(instance, b)
        assertEquals(b, p.get(instance))
        setter.call(instance, a)
        assertEquals(a, p.get(instance))
        setter.callBy(mapOf(setter.parameters[0] to instance, setter.parameters[1] to b))
        assertEquals(b, p.get(instance))
    } else {
        assertFalse(p is KMutableProperty1<*, *>)
    }
}

private fun assertAreEqual(a: Any, b: Any) {
    assertEquals(a, b)
    assertEquals(b, a)
    assertEquals(a.hashCode(), b.hashCode())
    assertEquals(a.toString(), b.toString())
}

fun box(): String {
    val valX1 = AX::class.members.single { it.name == "memberVal" } as KProperty1<*, *>
    check(valX1, false, "memberVal")
    assertEquals("val test.AX.memberVal: test.AX?", valX1.toString())
    // TODO (KT-87863): support callable references to properties inherited by get-/set-methods in Java
    // val valX2 = AX::memberVal
    // check(valX2, false, "memberVal")
    // assertAreEqual(valX1, valX2)

    val varX1 = AX::class.members.single { it.name == "memberVar" } as KProperty1<*, *>
    check(varX1, true, "memberVar")
    assertEquals("var test.AX.memberVar: test.AX?", varX1.toString())
    // TODO (KT-87863): support callable references to properties inherited by get-/set-methods in Java
    // val varX2 = AX::memberVar
    // check(varX2, true, "memberVar")
    // assertAreEqual(varX1, varX2)

    return "OK"
}
