// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: -CompanionBlocksAndExtensions
// FILE: test.kt
package my

import other.invoke
import other.invokable

class C
class C2 {
    companion object {
        operator fun invoke(x: Boolean) = x
        operator fun invoke(i: Int) = null
    }
}

class Invokable {
    operator fun invoke() {}
}

<!UNSUPPORTED_FEATURE, WRONG_MODIFIER_CONTAINING_DECLARATION!>companion<!> operator fun C.invoke(s: String) = s
<!UNSUPPORTED_FEATURE, WRONG_MODIFIER_CONTAINING_DECLARATION!>companion<!> operator fun C.invoke() = false
<!UNSUPPORTED_FEATURE, WRONG_MODIFIER_CONTAINING_DECLARATION!>companion<!> val C.invokable = Invokable()

fun testC() {
    val s: String <!INITIALIZER_TYPE_MISMATCH!>=<!> C(<!TOO_MANY_ARGUMENTS!>""<!>)
    val c: C = C()

    C.<!UNRESOLVED_REFERENCE!>invokable<!>()
}

fun testC2() {
    val i: Int <!INITIALIZER_TYPE_MISMATCH!>=<!> C2(1)
    val b: Boolean = C2(true)
    val c: C2 = C2()

    C2.<!UNRESOLVED_REFERENCE!>invokable<!>()
}

// FILE: other.kt
package other

import my.C2
import my.Invokable

<!UNSUPPORTED_FEATURE, WRONG_MODIFIER_CONTAINING_DECLARATION!>companion<!> operator fun C2.invoke(x: Int) = x
<!UNSUPPORTED_FEATURE, WRONG_MODIFIER_CONTAINING_DECLARATION!>companion<!> operator fun C2.invoke() = false
<!UNSUPPORTED_FEATURE, WRONG_MODIFIER_CONTAINING_DECLARATION!>companion<!> val C2.invokable = Invokable()

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, funWithExtensionReceiver, functionDeclaration, integerLiteral,
localProperty, objectDeclaration, operator, propertyDeclaration, propertyWithExtensionReceiver, stringLiteral */
