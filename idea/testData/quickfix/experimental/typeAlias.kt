// "Add '@OptIn(AliasMarker::class)' annotation to 'AliasMarkerUsage'" "true"
// COMPILER_ARGUMENTS: -Xuse-experimental=kotlin.Experimental
// WITH_RUNTIME
// ACTION: Add '-Xuse-experimental=AliasMarker' to module light_idea_test_case compiler arguments

@Experimental
annotation class AliasMarker

@AliasMarker
class AliasTarget

typealias AliasMarkerUsage = <caret>AliasTarget
