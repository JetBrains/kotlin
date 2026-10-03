// TARGET_BACKEND: JVM
// WITH_REFLECT

import kotlin.reflect.*
import kotlin.reflect.full.*
import kotlin.test.*

class Subject {
    fun noParams(): Unit = Unit
    fun oneParam(x: Int): String = x.toString()
    fun twoParams(x: Int, y: String): String = "$x$y"
    fun threeWithDefault(a: Int, b: String = "b", c: Boolean = true): String = "$a$b$c"
    fun varargParam(vararg xs: Int): Int = xs.sum()
    fun String.extensionFun(extra: Int): String = "$this:$extra"
    fun <T> genericFun(first: T, second: T): Boolean = first == second
    suspend fun suspendFun(value: Int): Int = value
}

fun box(): String {
    // No-params function: no parameters at all
    val noParams = Subject::class.memberFunctions.single { it.name == "noParams" }
    val noParamsList = noParams.parameters
    assertEquals(1, noParamsList.size) // only INSTANCE
    assertEquals(0, noParamsList[0].index)
    assertEquals(KParameter.Kind.INSTANCE, noParamsList[0].kind)

    // One-param function: INSTANCE at 0, VALUE at 1
    val oneParam = Subject::class.memberFunctions.single { it.name == "oneParam" }
    val oneParamsList = oneParam.parameters
    assertEquals(2, oneParamsList.size)
    assertEquals(0, oneParamsList[0].index)
    assertEquals(KParameter.Kind.INSTANCE, oneParamsList[0].kind)
    assertEquals(1, oneParamsList[1].index)
    assertEquals(KParameter.Kind.VALUE, oneParamsList[1].kind)
    assertEquals("x", oneParamsList[1].name)

    // Two-param function
    val twoParams = Subject::class.memberFunctions.single { it.name == "twoParams" }
    val twoParamsList = twoParams.parameters
    assertEquals(3, twoParamsList.size)
    assertEquals(0, twoParamsList[0].index) // INSTANCE
    assertEquals(1, twoParamsList[1].index) // x
    assertEquals(2, twoParamsList[2].index) // y
    assertEquals("x", twoParamsList[1].name)
    assertEquals("y", twoParamsList[2].name)

    // Three-param with defaults: indices are still contiguous
    val threeParams = Subject::class.memberFunctions.single { it.name == "threeWithDefault" }
    val threeList = threeParams.parameters
    assertEquals(4, threeList.size) // INSTANCE + a + b + c
    for (i in threeList.indices) {
        assertEquals(i, threeList[i].index,
            "Parameter at position $i should have index $i")
    }
    assertEquals("a", threeList[1].name); assertFalse(threeList[1].isOptional)
    assertEquals("b", threeList[2].name); assertTrue(threeList[2].isOptional)
    assertEquals("c", threeList[3].name); assertTrue(threeList[3].isOptional)

    // Vararg: still has an index
    val vararg = Subject::class.memberFunctions.single { it.name == "varargParam" }
    val varargList = vararg.parameters
    assertEquals(2, varargList.size) // INSTANCE + vararg
    assertEquals(0, varargList[0].index)
    assertEquals(1, varargList[1].index)
    assertTrue(varargList[1].isVararg)

    // Extension function: INSTANCE at 0, EXTENSION_RECEIVER at 1, VALUE at 2
    val extFun = Subject::class.memberExtensionFunctions.single { it.name == "extensionFun" }
    val extList = extFun.parameters
    assertEquals(3, extList.size)
    assertEquals(0, extList[0].index); assertEquals(KParameter.Kind.INSTANCE, extList[0].kind)
    assertEquals(1, extList[1].index); assertEquals(KParameter.Kind.EXTENSION_RECEIVER, extList[1].kind)
    assertEquals(2, extList[2].index); assertEquals(KParameter.Kind.VALUE, extList[2].kind)
    assertEquals("extra", extList[2].name)

    // Generic function: indices same as regular
    val genericFun = Subject::class.memberFunctions.single { it.name == "genericFun" }
    val genericList = genericFun.parameters
    assertEquals(3, genericList.size) // INSTANCE + first + second
    assertEquals(0, genericList[0].index)
    assertEquals(1, genericList[1].index); assertEquals("first", genericList[1].name)
    assertEquals(2, genericList[2].index); assertEquals("second", genericList[2].name)

    // Suspend function: indices unaffected by suspend
    val suspendFun = Subject::class.memberFunctions.single { it.name == "suspendFun" }
    val suspendList = suspendFun.parameters
    assertEquals(2, suspendList.size)
    assertEquals(0, suspendList[0].index)
    assertEquals(1, suspendList[1].index); assertEquals("value", suspendList[1].name)

    // Indices are consistent with findParameterByName
    val twoP = Subject::class.memberFunctions.single { it.name == "twoParams" }
    val xParam = twoP.findParameterByName("x")
    assertNotNull(xParam)
    assertEquals(1, xParam.index)
    val yParam = twoP.findParameterByName("y")
    assertNotNull(yParam)
    assertEquals(2, yParam.index)
    assertNull(twoP.findParameterByName("nonexistent"))

    return "OK"
}
