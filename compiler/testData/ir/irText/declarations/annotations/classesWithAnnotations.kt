// IGNORE_BACKEND: JS_IR

// KT-61141: absent enum fake_overrides: finalize, getDeclaringClass, clone
// IGNORE_BACKEND: NATIVE

annotation class TestAnn(val x: String)

@TestAnn("class")
class TestClass

@TestAnn("interface")
interface TestInterface

@TestAnn("object")
object TestObject

class Host {
    @TestAnn("companion")
    companion object TestCompanion
}

@TestAnn("enum")
enum class TestEnum

@TestAnn("annotation")
annotation class TestAnnotation
