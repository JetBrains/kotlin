package testing

annotation class Annotation<T>(val clazz: Class<T>)
class ATest

@[Annotation<ATest>(javaClass<<caret>ATest>())]
class BTest

// REF: (testing).ATest