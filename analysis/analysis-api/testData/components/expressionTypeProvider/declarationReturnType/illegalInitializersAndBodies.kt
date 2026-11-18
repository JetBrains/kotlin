// WITH_STDLIB
// LANGUAGE: +MultiPlatformProjects
expect val expectPropertyWithInitializer = 1
expect val expectPropertyWithGetter get() = 1
expect fun expectFunctionWithBody() = 1

abstract class AbstractClass {
    abstract val abstractPropertyWithInitializer = 1
    abstract fun abstractFunctionWithBody() = 1
}

interface Interface {
    val interfacePropertyWithInitializer = 1
}

external fun externalFunctionWithBody() = 1

lateinit var lateinitPropertyWithInitializer = "Hello"
