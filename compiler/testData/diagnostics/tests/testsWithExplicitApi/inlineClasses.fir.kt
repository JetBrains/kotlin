// !DIAGNOSTICS: -EXPERIMENTAL_FEATURE_WARNING
// !LANGUAGE: -JvmInlineValueClasses
// SKIP_TXT

inline class Value1(val inner: Int)
public inline class Value2(val inner: Int)
inline class Value3(public val inner: Int)
public inline class Value4(public val inner: Int)