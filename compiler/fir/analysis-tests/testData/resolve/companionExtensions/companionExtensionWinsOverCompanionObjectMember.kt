// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +CompanionBlocksAndExtensions
// FILE: Example.kt
package pack

import other.test2

class Example {
    companion object {
        fun test() = 1
        fun test2() = 1
        fun test3() = 1
    }

    companion {
        fun testInCompanionBlock() {
            val x: String = test()
            val y: String = test2()
            val z: Int = test3()
        }
    }
}

<!WRONG_MODIFIER_TARGET!>companion<!> fun Example.test() = ""

fun example() {
    val x: String = Example.test()
    val y: String = Example.test2()
    val z: Int = Example.test3()
}

<!WRONG_MODIFIER_TARGET!>companion<!> fun Example.testInCompanionBlock() {
    // Just for completeness, but companion object members aren't in scope when unqualified anyway.
    val x: String = test()
    val y: String = test2()
    val z: Int = <!UNRESOLVED_REFERENCE!>test3<!>()
}

// FILE: otherPackage.kt
package other

import pack.Example

<!WRONG_MODIFIER_TARGET!>companion<!> fun Example.test2() = ""

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, funWithExtensionReceiver, functionDeclaration, integerLiteral,
localProperty, objectDeclaration, propertyDeclaration, stringLiteral */
