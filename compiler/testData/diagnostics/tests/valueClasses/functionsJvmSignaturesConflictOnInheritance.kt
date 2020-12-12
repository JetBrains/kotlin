// FIR_IDENTICAL
// !LANGUAGE: +InlineClasses

package kotlin.jvm

annotation class JvmInline

@JvmInline
value class Name(val name: String)
@JvmInline
value class Password(val password: String)

interface NameVerifier {
    fun verify(name: Name)
}

interface PasswordVerifier {
    fun verify(password: Password)
}

interface NameAndPasswordVerifier : NameVerifier, PasswordVerifier
