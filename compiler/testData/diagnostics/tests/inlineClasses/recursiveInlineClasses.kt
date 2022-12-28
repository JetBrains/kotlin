// FIR_IDENTICAL
// !LANGUAGE: +InlineClasses, -JvmInlineValueClasses

inline class Test1(val x: <!VALUE_CLASS_CANNOT_BE_RECURSIVE!>Test1<!>)

inline class Test2A(val x: <!VALUE_CLASS_CANNOT_BE_RECURSIVE!>Test2B<!>)
inline class Test2B(val x: <!VALUE_CLASS_CANNOT_BE_RECURSIVE!>Test2A<!>)

inline class Test3A(val x: <!VALUE_CLASS_CANNOT_BE_RECURSIVE!>Test3B<!>)
inline class Test3B(val x: <!VALUE_CLASS_CANNOT_BE_RECURSIVE!>Test3C<!>)
inline class Test3C(val x: <!VALUE_CLASS_CANNOT_BE_RECURSIVE!>Test3A<!>)

inline class TestNullable(val x: <!VALUE_CLASS_CANNOT_BE_RECURSIVE!>TestNullable?<!>)

inline class TestRecursionInTypeArguments(val x: List<TestRecursionInTypeArguments>)

inline class TestRecursionInArray(val x: Array<TestRecursionInArray>)

inline class TestRecursionInUpperBounds<T : TestRecursionInUpperBounds<T>>(val x: T)

inline class Id<T>(val x: T)
inline class TestRecursionThroughId(val x: Id<TestRecursionThroughId>)