// TARGET_BACKEND: JVM
// WITH_REFLECT
// LANGUAGE: +CompanionBlocks

package test

import kotlin.reflect.*
import kotlin.reflect.jvm.*
import kotlin.test.*

interface I {
    companion {
        val blockVal = ""
        const val blockConst = ""
        var blockVar = 0
        val blockLazy by lazy { "lazy" }
        val blockDelegatedRef by ::blockVal
    }

    companion object {
        val inCompanion = 123
    }

    companion {
        val blockVal2 = ""
        const val blockConst2 = ""
        var blockVar2 = 0
        val blockLazy2 by lazy { "lazy" }
        val blockDelegatedRef2 by ::blockVal2
    }
}

fun box(): String {
    assertEquals("static final java.lang.String test.I\$\$PrivateFields1.blockVal", I::blockVal.javaField.toString())
    assertEquals("public static final java.lang.String test.I.blockConst", I::blockConst.javaField.toString())
    assertEquals("static int test.I\$\$PrivateFields1.blockVar", I::blockVar.javaField.toString())
    assertEquals("static final kotlin.Lazy test.I\$\$PrivateFields1.blockLazy\$delegate", I::blockLazy.javaField.toString())
    assertNull(I::blockDelegatedRef.javaField)

    assertEquals(I::blockVal.javaField?.kotlinProperty, I::blockVal)
    assertEquals(I::blockConst.javaField?.kotlinProperty, I::blockConst)
    assertEquals(I::blockVar.javaField?.kotlinProperty, I::blockVar)
    assertEquals(I::blockLazy.javaField?.kotlinProperty, I::blockLazy)

    assertEquals("static final java.lang.String test.I\$\$PrivateFields2.blockVal2", I::blockVal2.javaField.toString())
    assertEquals("public static final java.lang.String test.I.blockConst2", I::blockConst2.javaField.toString())
    assertEquals("static int test.I\$\$PrivateFields2.blockVar2", I::blockVar2.javaField.toString())
    assertEquals("static final kotlin.Lazy test.I\$\$PrivateFields2.blockLazy2\$delegate", I::blockLazy2.javaField.toString())
    assertNull(I::blockDelegatedRef2.javaField)

    assertEquals(I::blockVal2.javaField?.kotlinProperty, I::blockVal2)
    assertEquals(I::blockConst2.javaField?.kotlinProperty, I::blockConst2)
    assertEquals(I::blockVar2.javaField?.kotlinProperty, I::blockVar2)
    assertEquals(I::blockLazy2.javaField?.kotlinProperty, I::blockLazy2)

    return "OK"
}
