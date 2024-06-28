@file:OptIn(ExperimentalSubclassOptIn::class)

@RequiresOptIn
annotation class ApiMarker

@SubclassOptInRequired(ApiMarker::class)
interface Interface

typealias TypeAlias = Interface

class InterfaceInheritorB: TypeAlias