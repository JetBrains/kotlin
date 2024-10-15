// RUN_PIPELINE_TILL: BACKEND
fun foo() {
    class My {
        val x: Int
        init {
            var y: Int?
            y = 42
            x = y.hashCode()
        }
    }
}