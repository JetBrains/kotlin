import dependency.topLevelFun

// "Import" "true"
// ERROR: Unresolved reference: topLevelFun
val v = ::topLevelFun<caret>

/* IGNORE_FIR */