// RUN_PIPELINE_TILL: BACKEND
open class A

interface B<S, T : A>

class D : C<A>

typealias C<T> = B<T, A>

/* GENERATED_FIR_TAGS: classDeclaration, interfaceDeclaration, nullableType, typeAliasDeclaration,
typeAliasDeclarationWithTypeParameter, typeConstraint, typeParameter */
