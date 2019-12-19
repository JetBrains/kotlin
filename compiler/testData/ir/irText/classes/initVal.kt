// FIR_IDENTICAL
class TestInitValFromParameter(val x: Int)

class TestInitValInClass {
    val x = 0
}

class TestInitValInInitBlock {
    val x: Int
    init {
        x = 0
    }
}

class TestInitValInLambdaCalledOnce {
    val x: Int
    init {
        1.run {
            x = 0
        }
    }
}
