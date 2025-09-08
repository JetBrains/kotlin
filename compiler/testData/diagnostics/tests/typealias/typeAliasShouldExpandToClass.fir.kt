// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_PARAMETER -TOPLEVEL_TYPEALIASES_ONLY -UNSUPPORTED_FEATURE

typealias Dyn = <!TYPEALIAS_SHOULD_EXPAND_TO_CLASS, UNSUPPORTED!>dynamic<!>

typealias ToTypeParam1<T> = <!TYPEALIAS_SHOULD_EXPAND_TO_CLASS!>T<!>
typealias ToTypeParam2<T> = ToTypeParam1<T>
typealias ToTypeParam3<T1, T2> = ToTypeParam2<T1>
typealias ToTypeParam4 = ToTypeParam1<Any>

typealias ToFun1 = () -> Unit
typealias ToFun2<T> = (T) -> Unit

class Outer {
    typealias ToTypeParam1<T> = <!TYPEALIAS_SHOULD_EXPAND_TO_CLASS!>T<!>
    typealias ToTypeParam2<T> = ToTypeParam1<T>
    typealias ToTypeParam3<T1, T2> = ToTypeParam2<T1>
    typealias ToTypeParam4 = ToTypeParam1<Any>
}

/* GENERATED_FIR_TAGS: classDeclaration, flexibleType, functionalType, nullableType, typeAliasDeclaration,
typeAliasDeclarationWithTypeParameter, typeParameter */
