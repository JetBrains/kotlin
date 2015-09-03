// JAVAC_OPTIONS: -parameters

import kotlin.test.assertEquals

fun box(): String {
    val methodParam = J::foo.parameters.single()
    if (methodParam.name == null) return "Fail: method parameter has no name"
    assertEquals("methodParam", methodParam.name)

    val constructorParam = J::class.constructors.single().parameters.single()
    if (constructorParam.name == null) return "Fail: constructor parameter has no name"
    assertEquals("constructorParam", constructorParam.name)

    return "OK"
}
