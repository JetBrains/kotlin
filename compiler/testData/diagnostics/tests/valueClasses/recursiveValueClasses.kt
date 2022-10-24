// FIR_IDENTICAL
// !SKIP_JAVAC
// !LANGUAGE: +InlineClasses
// ALLOW_KOTLIN_PACKAGE

package kotlin.jvm

annotation class JvmInline

@JvmInline
value class Test1(val x: <!VALUE_CLASS_CANNOT_BE_RECURSIVE!>Test1<!>)

@JvmInline
value class Test2A(val x: <!VALUE_CLASS_CANNOT_BE_RECURSIVE!>Test2B<!>)
@JvmInline
value class Test2B(val x: <!VALUE_CLASS_CANNOT_BE_RECURSIVE!>Test2A<!>)

@JvmInline
value class Test3A(val x: <!VALUE_CLASS_CANNOT_BE_RECURSIVE!>Test3B<!>)
@JvmInline
value class Test3B(val x: <!VALUE_CLASS_CANNOT_BE_RECURSIVE!>Test3C<!>)
@JvmInline
value class Test3C(val x: <!VALUE_CLASS_CANNOT_BE_RECURSIVE!>Test3A<!>)

@JvmInline
value class TestNullable(val x: <!VALUE_CLASS_CANNOT_BE_RECURSIVE!>TestNullable?<!>)

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
