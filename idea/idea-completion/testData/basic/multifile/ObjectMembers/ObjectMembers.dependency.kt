package test

object KotlinObject {
    fun funFromObject() { }
    private fun privateFun(){}
}

class KotlinClass {
    companion object SomeName {
        fun funFromCompanionObject() { }
        private fun privateFun(){}
    }
}

class AnotherKotlinClass {
    private companion object {
        fun funFromPrivateCompanionObject() { }
    }
}
