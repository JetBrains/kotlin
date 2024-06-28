// FIR_IDENTICAL
// LANGUAGE: +InlineClasses, -JvmInlineValueClasses

inline class Name(val name: String)
inline class Password(val password: String)

interface NameVerifier {
    fun verify(name: Name)
}

interface PasswordVerifier {
    fun verify(password: Password)
}

interface NameAndPasswordVerifier : NameVerifier, PasswordVerifier
