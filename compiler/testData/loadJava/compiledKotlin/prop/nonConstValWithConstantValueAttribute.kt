// PLATFORM_DEPENDANT_METADATA
// !LANGUAGE: -NoConstantValueAttributeForNonConstVals
// IGNORE_BACKEND: JVM_IR
//ALLOW_AST_ACCESS

package test
val nonConstVal1 = 1

class C {
    val nonConstVal2 = 2

    companion object {
        val nonConstVal3 = 3
    }
}

interface I {
    companion object {
        val nonConstVal4 = 4
    }
}
