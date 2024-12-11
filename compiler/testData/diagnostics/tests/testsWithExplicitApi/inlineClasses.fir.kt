// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -EXPERIMENTAL_FEATURE_WARNING -INLINE_CLASS_DEPRECATED
// SKIP_TXT

<!NO_EXPLICIT_VISIBILITY_IN_API_MODE!>inline class Value1<!>(val inner: Int)
public inline class Value2(val inner: Int)
<!NO_EXPLICIT_VISIBILITY_IN_API_MODE!>inline class Value3<!>(public val inner: Int)
public inline class Value4(public val inner: Int)
