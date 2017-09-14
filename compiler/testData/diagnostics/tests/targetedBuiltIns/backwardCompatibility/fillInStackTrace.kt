// JAVAC_EXPECTED_FILE
class ControlFlowException : Exception("") {
    fun fillInStackTrace() = this
}