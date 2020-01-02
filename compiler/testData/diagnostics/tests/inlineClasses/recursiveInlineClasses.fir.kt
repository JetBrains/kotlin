// !LANGUAGE: +InlineClasses

inline class Test1(val x: Test1)

inline class Test2A(val x: Test2B)
inline class Test2B(val x: Test2A)

inline class Test3A(val x: Test3B)
inline class Test3B(val x: Test3C)
inline class Test3C(val x: Test3A)

inline class TestNullable(val x: TestNullable?)

inline class TestRecursionInTypeArguments(val x: List<TestRecursionInTypeArguments>)

inline class TestRecursionInArray(val x: Array<TestRecursionInArray>)

inline class TestRecursionInUpperBounds<T : TestRecursionInUpperBounds<T>>(val x: T)

inline class Id<T>(val x: T)
inline class TestRecursionThroughId(val x: Id<TestRecursionThroughId>)