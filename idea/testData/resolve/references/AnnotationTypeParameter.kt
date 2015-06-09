package testing

annotation class Annotation<T>(val clazz: Class<T>)
class ATest

@[Annotation<<caret>ATest>(javaClass<ATest>())]
class BTest

// REF: (testing).ATest