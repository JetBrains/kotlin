// RUN_PIPELINE_TILL: CODEGEN

@file:Suppress("INLINE_CLASS_DEPRECATED", <!ERROR_SUPPRESSION!>"INLINE_CLASS_DEPRECATED_ERROR"<!>)

inline class Name(val name: String)
inline class Password(val password: String)

interface NameVerifier {
    fun verify(name: Name)
}

interface PasswordVerifier {
    fun verify(password: Password)
}

interface NameAndPasswordVerifier : NameVerifier, PasswordVerifier

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, interfaceDeclaration, primaryConstructor,
propertyDeclaration */
