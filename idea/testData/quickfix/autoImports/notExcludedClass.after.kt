import somePackage.NotExcludedClass

// "Import" "true"
// ERROR: Unresolved reference: NotExcludedClass

val x = <caret>NotExcludedClass()