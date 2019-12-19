// IGNORE_BACKEND: ANY_FIR
class TestInitValInLambdaCalledOnce {
    val x: Int
    init {
        1.run {
            x = 0
        }
    }
}
