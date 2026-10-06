package test

abstract class AbstractParent
class ConcreteOne : AbstractParent()
class ConcreteTwo : AbstractParent()

abstract class AbstractCodeMetaInfoTest {
    open fun getConfigurations(): List<AbstractParent> = listOf(ConcreteOne(), ConcreteTwo())
}

abstract class AbstractLineMarkerCodeMetaInfoTest : AbstractCodeMetaInfoTest() {
    override fun getConfigurations(): List<ConcreteTwo> = listOf(ConcreteTwo())
}
