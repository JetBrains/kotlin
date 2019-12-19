// IGNORE_BACKEND_FIR: ANY
class TestInitValInLambdaCalledOnce {
    val x: Int
    init {
        1.run {
            x = 0
        }
    }
}
