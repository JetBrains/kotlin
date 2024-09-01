// LANGUAGE: -ForbidReifiedTypeParametersOnTypeAliases
// ISSUE: KT-20798
// RENDER_DIAGNOSTICS_FULL_TEXT

class Foo<T>

typealias Alias<reified R> = Foo<R>
