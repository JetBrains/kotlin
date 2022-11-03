// FIR_DISABLE_LAZY_RESOLVE_CHECKS
// FIR_IDENTICAL
// JAVAC_EXPECTED_FILE
class ControlFlowException : Exception("") {
    fun fillInStackTrace() = this
}