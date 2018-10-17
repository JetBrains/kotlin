// "Safe delete 'Marker'" "false"
// COMPILER_ARGUMENTS: -Xuse-experimental=kotlin.Experimental -Xuse-experimental=test.Marker
// WITH_RUNTIME
// ACTION: Rename file to Marker.kt
// TOOL: org.jetbrains.kotlin.idea.inspections.UnusedSymbolInspection

package test

@Experimental
annotation class <caret>Marker
