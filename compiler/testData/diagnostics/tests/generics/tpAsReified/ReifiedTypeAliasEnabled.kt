// RUN_PIPELINE_TILL: SOURCE
// LANGUAGE: +ForbidReifiedTypeParametersOnTypeAliases
// ISSUE: KT-20798

class Foo<T>

typealias Alias<reified R> = Foo<R>
