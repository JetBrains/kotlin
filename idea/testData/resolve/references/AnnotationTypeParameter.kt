package testing

annotation class Annotation<T>(val clazz: Class<T>)
class ATest

@[Annotation<<caret>ATest>(ATest::class.java)]
class BTest

// REF: (testing).ATest