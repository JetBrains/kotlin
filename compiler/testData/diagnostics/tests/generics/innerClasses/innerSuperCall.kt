// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
open class Super<T> {
    inner open class Inner {
    }
}

class Sub : Super<String>() {
    // TODO: it would be nice to have a possibility to omit explicit type argument in supertype
    inner class SubInner : Super<String>.Inner() {}
}

/* GENERATED_FIR_TAGS: classDeclaration, inner, nullableType, typeParameter */
