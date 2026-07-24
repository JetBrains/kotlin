// LANGUAGE: +ContextParameters
// TARGET_BACKEND: JVM
// WITH_REFLECT
// FILE: J.java
public class J extends A {
    @Override
    public void foo(int x, int y) {}
}

// FILE: box.kt
import kotlin.test.*

open class A {
    open fun foo(x: Int, y: Int = 1) {}
}

class B : A() {
    override fun foo(x: Int, y: Int) {}
}

class C : A()


fun Int.extFun() {}

class Z {
    context(c: String)
    fun context() {}
}

fun box(): String {
    assertEquals(listOf(false, false, true), A::foo.parameters.map { it.isOptional })
    assertEquals(listOf(false, false, true), B::foo.parameters.map { it.isOptional })
    assertEquals(listOf(false, false, true), C::foo.parameters.map { it.isOptional })

    if (Class.forName("kotlin.reflect.jvm.internal.SystemPropertiesKt").getMethod("getUseK1Implementation").invoke(null) == true) {
        assertEquals(listOf(false, false, true), J::foo.parameters.map { it.isOptional })
    } else {
        // TODO(KT-86692): isOptional for new Java method implementation doesn't consider inherited default values
        assertEquals(listOf(false, false, false), J::foo.parameters.map { it.isOptional })
    }

    assertFalse(Int::extFun.parameters.single().isOptional)

    val context = Z::class.members.single { it.name == "context" }
    assertEquals(listOf(false, false), context.parameters.map { it.isOptional })

    return "OK"
}
