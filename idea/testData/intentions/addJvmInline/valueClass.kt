// WITH_RUNTIME

// ERROR: Value classes without @JvmInline annotation are not supported yet
// SKIP_ERRORS_AFTER

<caret>value class VC(val i: Int)