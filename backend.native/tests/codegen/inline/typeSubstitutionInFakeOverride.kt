package codegen.inline.typeSubstitutionInFakeOverride

import kotlin.test.*

abstract class A {
    inline fun <reified T : Any> baz(): String {
        return T::class.simpleName!!
    }
}

class B : A() {
    fun bar(): String {
        return baz<OK>()
    }
}

class OK

@Test fun runTest() {
    println(B().bar())
}