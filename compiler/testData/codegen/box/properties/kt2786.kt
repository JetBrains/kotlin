// JVM_ABI_K1_K2_DIFF: KT-63828
interface FooTrait {
        val propertyTest: String
}

class FooDelegate: FooTrait {
        override val propertyTest: String = "OK"
}

class DelegateTest(): FooTrait by FooDelegate() {
  fun test() = propertyTest
}

fun box()  = DelegateTest().test()
