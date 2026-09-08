// ISSUE: KT-88889
// LOOK_UP_FOR_ELEMENT_OF_TYPE: KtFile
// CHECKER_KIND: EXTENDED

// The file's structure element skips the top-level declarations, as each of them owns its own structure element, so nothing resolves
// them. 'FirConflictsDeclarationChecker' would, as a side effect of the package scope lookup, but it is a common checker and only
// extended ones are requested here.
<caret>package pack

inline fun topLevelFunction(block: () -> Unit) {
    block()
}
