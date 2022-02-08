// FIR_IDENTICAL
// !DIAGNOSTICS: -UNUSED_PARAMETER -PLATFORM_CLASS_MAPPED_TO_KOTLIN


// Default methods from Java interfaces are NOT overridden by delegation.
// So, in the example below 'TestNoDelegationToDefaultMethods#replace' implicitly overrides a method from 'java.util.Map' (which is ok),
// but not a method from 'WithDelegation' (would be an error).
open class WithDelegation(val m: Map<String, String>) : Map<String, String> by m

class TestNoDelegationToDefaultMethods(m: Map<String, String>): WithDelegation(m) {
    fun <!VIRTUAL_MEMBER_HIDDEN!>containsKey<!>(key: String): Boolean = TODO()

    fun getOrDefault(key: String, defaultValue: String): String = TODO()

    fun replace(key: String, value: String): String? = TODO()
}



interface IBaseWithKotlinDeclaration : Map<String, String> {
    fun replace(key: String, value: String): String?
}

abstract class WithDelegation2(val m: Map<String, String>) : Map<String, String> by m, IBaseWithKotlinDeclaration

abstract class TestNoDelegationToDefaultMethods2(m: Map<String, String>): WithDelegation2(m) {
    fun <!VIRTUAL_MEMBER_HIDDEN!>containsKey<!>(key: String): Boolean = TODO()

    fun getOrDefault(key: String, defaultValue: String): String = TODO()

    // VIRTUAL_MEMBER_HIDDEN: hides member declaration inherited from a Kotlin interface
    fun <!VIRTUAL_MEMBER_HIDDEN!>replace<!>(key: String, value: String): String? = TODO()
}
