deprecated("does nothing good")
fun Any.doNothing() = this.toString()  // "this" should not be marked as deprecated despite it referes to deprecated function

// NO_CHECK_INFOS
// NO_CHECK_WEAK_WARNINGS