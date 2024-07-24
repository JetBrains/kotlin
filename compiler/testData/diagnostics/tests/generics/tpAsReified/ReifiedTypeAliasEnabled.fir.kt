// LANGUAGE: +ForbidReifiedTypeParametersOnTypeAliases
// ISSUE: KT-20798

class Foo<T>

typealias Alias<<!REIFIED_TYPE_PARAMETER_ON_ALIAS_ERROR!>reified<!> R> = Foo<R>
