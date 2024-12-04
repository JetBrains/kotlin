// RUN_PIPELINE_TILL: BACKEND
class TestInitValInLambdaCalledOnce {
    val x: Int
    init {
        1.run {
            x = 0
        }
    }
}