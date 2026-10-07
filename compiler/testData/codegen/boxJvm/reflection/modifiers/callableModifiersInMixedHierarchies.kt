// TARGET_BACKEND: JVM
// WITH_REFLECT
// FILE: JBase.java

public abstract class JBase {
    public String openM() { return "JBase.openM"; }
    public final String finalM() { return "JBase.finalM"; }
    public abstract String abstractM();
    protected String protectedM() { return "JBase.protectedM"; }
    public String plus(int x) { return "JBase.plus" + x; }
}

// FILE: JInterface.java

public interface JInterface {
    String abstractM();
    default String defaultM() { return "JInterface.defaultM"; }
}

// FILE: JFromKotlin.java

public class JFromKotlin extends KBase {}

// FILE: JFromKotlinInterface.java

public interface JFromKotlinInterface extends KInterface {}

// FILE: JFromKotlinInterfaceImpl.java

public class JFromKotlinInterfaceImpl implements JFromKotlinInterface {
    public String abstractFun() { return "JFromKotlinInterfaceImpl.abstractFun"; }
}

// FILE: box.kt

// Modality, visibility and other modifiers of Java callables inherited by Kotlin classes and vice versa.
// See callableModality.kt, javaCallableModality.kt, callableVisibility.kt, javaVisibility.kt for non-mixed hierarchies.

import kotlin.coroutines.*
import kotlin.reflect.KClass
import kotlin.reflect.KCallable
import kotlin.reflect.KFunction
import kotlin.reflect.KVisibility
import kotlin.reflect.full.IllegalCallableAccessException
import kotlin.reflect.full.callSuspend
import kotlin.reflect.jvm.isAccessible
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.test.assertFalse

open class KBase {
    open fun openFun() = "KBase.openFun"
    fun finalFun() = "KBase.finalFun"
    protected open fun protectedFun() = "KBase.protectedFun"
    operator fun plus(x: Int) = "KBase.plus$x"
    infix fun infix(x: Int) = "KBase.infix$x"
    inline fun inline() = "KBase.inline"
    suspend fun suspend() = "KBase.suspend"
}

interface KInterface {
    fun abstractFun(): String
    fun defaultFun() = "KInterface.defaultFun"
}

abstract class KFromJava : JBase()

class KFromJavaImpl : KFromJava() {
    override fun abstractM() = "KFromJavaImpl.abstractM"
}

open class KOverridesJava : JBase() {
    final override fun openM() = "KOverridesJava.openM"
    override fun abstractM() = "KOverridesJava.abstractM"
    public override fun protectedM() = "KOverridesJava.protectedM"
}

interface KFromJavaInterface : JInterface

class KFromJavaInterfaceImpl : KFromJavaInterface {
    override fun abstractM() = "KFromJavaInterfaceImpl.abstractM"
}

private fun KClass<*>.member(name: String): KCallable<*> = members.single { it.name == name }
private fun KClass<*>.function(name: String): KFunction<*> = member(name) as KFunction<*>

private fun <T> runSuspend(block: suspend () -> T): T {
    var result: Result<T>? = null
    block.startCoroutine(Continuation(EmptyCoroutineContext) { result = it })
    return result!!.getOrThrow()
}

private fun checkFinal(callable: KCallable<*>) {
    assertTrue(callable.isFinal)
    assertFalse(callable.isOpen)
    assertFalse(callable.isAbstract)
}

private fun checkOpen(callable: KCallable<*>) {
    assertFalse(callable.isFinal)
    assertTrue(callable.isOpen)
    assertFalse(callable.isAbstract)
}

private fun checkAbstract(callable: KCallable<*>) {
    assertFalse(callable.isFinal)
    assertFalse(callable.isOpen)
    assertTrue(callable.isAbstract)
}

fun box(): String {
    // Kotlin class inheriting Java methods.
    KFromJava::class.member("openM").let {
        checkOpen(it)
        assertEquals(KVisibility.PUBLIC, it.visibility)
        assertEquals("JBase.openM", it.call(KFromJavaImpl()))
    }
    KFromJava::class.member("finalM").let {
        checkFinal(it)
        assertEquals("JBase.finalM", it.call(KFromJavaImpl()))
    }
    KFromJava::class.member("abstractM").let {
        checkAbstract(it)
        assertEquals("KFromJavaImpl.abstractM", it.call(KFromJavaImpl()))
    }
    // Java's protected also allows access in the same package, so it cannot be represented in Kotlin.
    KFromJava::class.member("protectedM").let {
        checkOpen(it)
        assertEquals(null, it.visibility)
        assertFailsWith<IllegalCallableAccessException> { it.call(KFromJavaImpl()) }
        it.isAccessible = true
        assertEquals("JBase.protectedM", it.call(KFromJavaImpl()))
    }

    // Kotlin class overriding Java methods.
    KOverridesJava::class.member("openM").let {
        checkFinal(it)
        assertEquals("KOverridesJava.openM", it.call(KOverridesJava()))
    }
    KOverridesJava::class.member("abstractM").let {
        checkOpen(it)
        assertEquals("KOverridesJava.abstractM", it.call(KOverridesJava()))
    }
    KOverridesJava::class.member("protectedM").let {
        checkOpen(it)
        assertEquals(KVisibility.PUBLIC, it.visibility)
        assertEquals("KOverridesJava.protectedM", it.call(KOverridesJava()))
    }
    // The Java member dispatches to the Kotlin override.
    assertEquals("KOverridesJava.openM", JBase::class.member("openM").call(KOverridesJava()))

    // For Java methods inherited by Kotlin classes, `isOperator` is computed from the method's name and signature.
    KFromJava::class.function("plus").let {
        assertTrue(it.isOperator)
        assertEquals("JBase.plus1", it.call(KFromJavaImpl(), 1))
    }

    // Java class inheriting Kotlin functions.
    JFromKotlin::class.member("openFun").let {
        checkOpen(it)
        assertEquals("KBase.openFun", it.call(JFromKotlin()))
    }
    JFromKotlin::class.member("finalFun").let {
        checkFinal(it)
        assertEquals("KBase.finalFun", it.call(JFromKotlin()))
    }
    JFromKotlin::class.member("protectedFun").let {
        checkOpen(it)
        assertEquals(KVisibility.PROTECTED, it.visibility)
        assertFailsWith<IllegalCallableAccessException> { it.call(JFromKotlin()) }
        it.isAccessible = true
        assertEquals("KBase.protectedFun", it.call(JFromKotlin()))
    }
    // Modifiers of Kotlin functions inherited by Java classes come from the Kotlin declaration.
    JFromKotlin::class.function("plus").let {
        assertTrue(it.isOperator)
        assertEquals("KBase.plus2", it.call(JFromKotlin(), 2))
    }
    JFromKotlin::class.function("infix").let {
        assertTrue(it.isInfix)
        assertEquals("KBase.infix3", it.call(JFromKotlin(), 3))
    }
    JFromKotlin::class.function("inline").let {
        assertTrue(it.isInline)
        assertEquals("KBase.inline", it.call(JFromKotlin()))
    }
    JFromKotlin::class.function("suspend").let {
        assertTrue(it.isSuspend)
        assertEquals("KBase.suspend", runSuspend { it.callSuspend(JFromKotlin()) })
    }

    // Kotlin interface inheriting Java interface methods.
    KFromJavaInterface::class.member("abstractM").let {
        checkAbstract(it)
        assertEquals("KFromJavaInterfaceImpl.abstractM", it.call(KFromJavaInterfaceImpl()))
    }
    KFromJavaInterface::class.member("defaultM").let {
        checkOpen(it)
        assertEquals("JInterface.defaultM", it.call(KFromJavaInterfaceImpl()))
    }

    // Java interface inheriting Kotlin interface functions.
    JFromKotlinInterface::class.member("abstractFun").let {
        checkAbstract(it)
        assertEquals("JFromKotlinInterfaceImpl.abstractFun", it.call(JFromKotlinInterfaceImpl()))
    }
    JFromKotlinInterface::class.member("defaultFun").let {
        checkOpen(it)
        assertEquals("KInterface.defaultFun", it.call(JFromKotlinInterfaceImpl()))
    }

    return "OK"
}
