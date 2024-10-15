// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// JAVAC_EXPECTED_FILE
class ControlFlowException : Exception("") {
    fun fillInStackTrace() = this
}