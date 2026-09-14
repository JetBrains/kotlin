// CLASS_TYPE_RENDERING_MODE: EXPANSION_WITH_ABBREVIATION_COMMENT
class Base<T>

typealias Alias<T> = Base<T>
typealias StringAlias = Alias<String>

fun accept(direct: Alias<Int>, chained: StringAlias, plain: Base<Boolean>) {}
