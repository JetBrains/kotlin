// "Add '@OptIn(AliasMarker::class)' annotation to 'AliasMarkerUsage'" "true"
// COMPILER_ARGUMENTS: -Xopt-in=kotlin.RequiresOptIn
// WITH_RUNTIME
// ACTION: Add '-Xopt-in=AliasMarker' to module light_idea_test_case compiler arguments

@RequiresOptIn
annotation class AliasMarker

@AliasMarker
class AliasTarget

typealias AliasMarkerUsage = <caret>AliasTarget
