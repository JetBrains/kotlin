// !LANGUAGE: +InlineClasses

package kotlin.jvm

annotation class JvmInline

@JvmInline
value class Test1(val x: Test1)

@JvmInline
value class Test2A(val x: Test2B)
@JvmInline
value class Test2B(val x: Test2A)

@JvmInline
value class Test3A(val x: Test3B)
@JvmInline
value class Test3B(val x: Test3C)
@JvmInline
value class Test3C(val x: Test3A)

@JvmInline
value class TestNullable(val x: TestNullable?)

@JvmInline
value class TestRecursionInTypeArguments(val x: List<TestRecursionInTypeArguments>)

@JvmInline
value class TestRecursionInArray(val x: Array<TestRecursionInArray>)

@JvmInline
value class TestRecursionInUpperBounds<T : TestRecursionInUpperBounds<T>>(val x: T)

@JvmInline
value class Id<T>(val x: T)
@JvmInline
value class TestRecursionThroughId(val x: Id<TestRecursionThroughId>)