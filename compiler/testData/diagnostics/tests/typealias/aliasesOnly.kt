// RUN_PIPELINE_TILL: BACKEND
// DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_PARAMETER -TOPLEVEL_TYPEALIASES_ONLY -UNSUPPORTED_FEATURE

typealias S = String

class C {
    typealias SS = String
    typealias SF<T> = (T) -> String
}

/* GENERATED_FIR_TAGS: classDeclaration, functionalType, nullableType, typeAliasDeclaration,
typeAliasDeclarationWithTypeParameter, typeParameter */
