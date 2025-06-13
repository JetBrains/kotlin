// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// KT-49078

class CidrMemoryData<T> {
    interface Data<out T>
    abstract class AbstractData<out T, E : Data<T>>
}

/* GENERATED_FIR_TAGS: classDeclaration, interfaceDeclaration, nestedClass, nullableType, out, typeConstraint,
typeParameter */
