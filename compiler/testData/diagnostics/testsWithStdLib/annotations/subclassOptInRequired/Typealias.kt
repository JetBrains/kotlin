// SUPPRESS_NO_TYPE_ALIAS_EXPANSION_MODE: TBD
@file:OptIn(ExperimentalSubclassOptIn::class)

@RequiresOptIn
annotation class ApiMarker

@SubclassOptInRequired(ApiMarker::class)
interface Interface

typealias TypeAlias = Interface

class InterfaceInheritorB: TypeAlias
