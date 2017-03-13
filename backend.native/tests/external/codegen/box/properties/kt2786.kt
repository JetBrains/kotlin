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
