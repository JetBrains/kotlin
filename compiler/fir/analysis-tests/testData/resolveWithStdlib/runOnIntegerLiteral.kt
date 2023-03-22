// IGNORE_REVERSED_RESOLVE
class TestInitValInLambdaCalledOnce {
    val x: Int
    init {
        1.run {
            x = 0
        }
    }
}