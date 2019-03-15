// "Make 'getInternal' internal" "true"
// ERROR: 'public' function exposes its 'internal' return type Internal

internal expect class Internal

expect fun <caret>getInternal(): Internal