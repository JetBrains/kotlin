// LANGUAGE: +ContextParameters +CallableReferencesToContextual
// TARGET_BACKEND: JVM_IR
// WITH_REFLECT
// ISSUE: KT-86452

import kotlin.reflect.KVisibility
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

annotation class Ann

@Ann
context(c: String)
internal fun annotated(x: Int): String = c + x

class Cls {
    @Ann
    context(c: String)
    fun member(): String = c
}

fun box(): String {
    context("ctx") {
        val f = ::annotated
        assertEquals("annotated", f.name)
        assertEquals(String::class, f.returnType.classifier)
        assertEquals(KVisibility.INTERNAL, f.visibility)
        assertFalse(f.isSuspend)
        assertTrue(f.annotations.any { it is Ann })
        assertEquals("ctx5", f.call(5))

        val m = Cls::member
        assertEquals("member", m.name)
        assertEquals(String::class, m.returnType.classifier)
        assertEquals(KVisibility.PUBLIC, m.visibility)
        assertTrue(m.annotations.any { it is Ann })
    }
    return "OK"
}
