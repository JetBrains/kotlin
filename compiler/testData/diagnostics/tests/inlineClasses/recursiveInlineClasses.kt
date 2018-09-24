// !LANGUAGE: +InlineClasses

inline class Test1(val x: <!INLINE_CLASS_CANNOT_BE_RECURSIVE!>Test1<!>)

inline class Test2A(val x: <!INLINE_CLASS_CANNOT_BE_RECURSIVE!>Test2B<!>)
inline class Test2B(val x: <!INLINE_CLASS_CANNOT_BE_RECURSIVE!>Test2A<!>)

inline class Test3A(val x: <!INLINE_CLASS_CANNOT_BE_RECURSIVE!>Test3B<!>)
inline class Test3B(val x: <!INLINE_CLASS_CANNOT_BE_RECURSIVE!>Test3C<!>)
inline class Test3C(val x: <!INLINE_CLASS_CANNOT_BE_RECURSIVE!>Test3A<!>)

inline class TestNullable(val x: <!INLINE_CLASS_CANNOT_BE_RECURSIVE!>TestNullable?<!>)

inline class TestRecursionInTypeArguments(val x: List<TestRecursionInTypeArguments>)

inline class TestRecursionInArray(val x: Array<TestRecursionInArray>)

inline class TestRecursionInUpperBounds<T : TestRecursionInUpperBounds<T>>(val x: <!INLINE_CLASS_HAS_INAPPLICABLE_PARAMETER_TYPE!>T<!>)

inline class Id<T>(val x: <!INLINE_CLASS_HAS_INAPPLICABLE_PARAMETER_TYPE!>T<!>)
inline class TestRecursionThroughId(val x: Id<TestRecursionThroughId>)