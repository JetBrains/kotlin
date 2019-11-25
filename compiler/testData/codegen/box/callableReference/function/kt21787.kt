// IGNORE_BACKEND_FIR: JVM_IR
// KJS_WITH_FULL_RUNTIME
// WITH_RUNTIME
class Container {
    var id: Int? = null
}

class TestClass {

    private fun createContainer(id: Int): Container { val q = Container(); q.id = id; return q }
    fun createContainers1(from: Int = 0, to: Int = 100) = (from .. to).map(::createContainer)
    fun createContainers2(from: Int = 0, to: Int = 100): List<Container> { return (from .. to).map(::createContainer) }
}

fun box(): String {
    val testClass = TestClass()
    val containers1 = testClass.createContainers1().size
    if (containers1 != 101) return "fail 1: $containers1"

    val containers2 = testClass.createContainers2().size
    if (containers2 != 101) return "fail 2: $containers2"

    return "OK"
}