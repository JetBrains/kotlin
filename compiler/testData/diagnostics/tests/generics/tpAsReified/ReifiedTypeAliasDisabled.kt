// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: -ForbidReifiedTypeParametersOnTypeAliases
// ISSUE: KT-20798
// RENDER_DIAGNOSTICS_FULL_TEXT

class Foo<T>

typealias Alias<reified R> = Foo<R>

/* GENERATED_FIR_TAGS: classDeclaration, nullableType, reified, typeAliasDeclaration,
typeAliasDeclarationWithTypeParameter, typeParameter */
