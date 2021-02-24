// TARGET_BACKEND: JVM
// This test checks that JVM-specific static initialization behavior is preserved in JVM_IR.

var testObjectInit = false
var testClassCompanionInit = false
var testInterfaceCompanionInit = false

fun use(x: Int) {}

object TestObject {
    init {
        testObjectInit = true
    }
    const val x = 42
}

fun getTestObject() = TestObject

class TestClassCompanion {
    companion object {
        init {
            testClassCompanionInit = true
        }
        const val x = 42
    }
}

fun getTestClassCompanion() = TestClassCompanion

class TestInterfaceCompanion {
    companion object {
        init {
            testInterfaceCompanionInit = true
        }
        const val x = 42
    }
}

fun getInterfaceCompanion() = TestInterfaceCompanion

fun box(): String {
    use(TestObject.x)
    if (testObjectInit)
        throw Exception("use(TestObject.x)")

    use((TestObject).x)
    if (testObjectInit)
        throw Exception("use((TestObject).x)")

    use(getTestObject().x)
    if (!testObjectInit)
        throw Exception("use(getTestObject().x)")

    use(TestClassCompanion.x)
    if (testClassCompanionInit)
        throw Exception("use(TestClassCompanion.x)")

    use((TestClassCompanion).x)
    if (testClassCompanionInit)
        throw Exception("use((TestClassCompanion).x)")

    use(getTestClassCompanion().x)
    if (!testClassCompanionInit)
        throw Exception("use(getTestClassCompanion().x)")

    use(TestInterfaceCompanion.x)
    if (testInterfaceCompanionInit)
        throw Exception("use(TestInterfaceCompanion.x)")

    use((TestInterfaceCompanion).x)
    if (testInterfaceCompanionInit)
        throw Exception("use((TestInterfaceCompanion).x)")

    use(getInterfaceCompanion().x)
    if (!testInterfaceCompanionInit)
        throw Exception("use(getInterfaceCompanion().x)")

    return "OK"
}