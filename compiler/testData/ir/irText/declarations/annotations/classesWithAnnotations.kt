// FIR_IDENTICAL
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
