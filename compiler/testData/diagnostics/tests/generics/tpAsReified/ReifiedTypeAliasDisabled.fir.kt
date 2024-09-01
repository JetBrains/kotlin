// LANGUAGE: -ForbidReifiedTypeParametersOnTypeAliases
// ISSUE: KT-20798
// RENDER_DIAGNOSTICS_FULL_TEXT

class Foo<T>

typealias Alias<<!REIFIED_TYPE_PARAMETER_ON_ALIAS_WARNING!>reified<!> R> = Foo<R>
