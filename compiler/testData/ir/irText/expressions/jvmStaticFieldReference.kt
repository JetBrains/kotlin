fun testFun() {
    System.out.println("testFun")
}

var testProp: Any
    get() {
        System.out.println("testProp/get")
        return 42
    }
    set(value) {
        System.out.println("testProp/set")
    }

class TestClass {
    val test = when {
        else -> {
            System.out.println("TestClass/test")
            42
        }
    }

    init {
        System.out.println("TestClass/init")
    }
}