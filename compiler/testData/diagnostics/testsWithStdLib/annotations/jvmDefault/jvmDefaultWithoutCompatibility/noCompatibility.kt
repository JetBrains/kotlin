// RUN_PIPELINE_TILL: FRONTEND
// JVM_DEFAULT_MODE: no-compatibility

@JvmDefaultWithoutCompatibility
interface A<T> {
    fun test(p: T) {}
}

@JvmDefaultWithoutCompatibility
class B : A<String> {}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, interfaceDeclaration, nullableType, typeParameter */
