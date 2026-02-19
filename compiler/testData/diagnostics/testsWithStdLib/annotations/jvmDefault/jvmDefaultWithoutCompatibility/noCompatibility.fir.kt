// RUN_PIPELINE_TILL: FRONTEND
// JVM_DEFAULT_MODE: no-compatibility

<!JVM_DEFAULT_WITHOUT_COMPATIBILITY_NOT_IN_ENABLE_MODE!>@JvmDefaultWithoutCompatibility<!>
interface A<T> {
    fun test(p: T) {}
}

<!JVM_DEFAULT_WITHOUT_COMPATIBILITY_NOT_IN_ENABLE_MODE!>@JvmDefaultWithoutCompatibility<!>
class B : A<String> {}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, interfaceDeclaration, nullableType, typeParameter */
