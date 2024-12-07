// RUN_PIPELINE_TILL: BACKEND
enum class EC {
    A, B;
    companion object {
        fun u(ec: EC): Boolean {
            return when (ec) {
                A -> true
                B -> false
            }
        }
    }
}
