// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -INLINE_CLASS_DEPRECATED -INLINE_CLASS_DEPRECATED_ERROR

<!NO_EXPLICIT_VISIBILITY_IN_API_MODE!>inline class Value1<!>(<!NO_EXPLICIT_VISIBILITY_IN_API_MODE!>val inner<!>: Int)
public inline class Value2(<!NO_EXPLICIT_VISIBILITY_IN_API_MODE!>val inner<!>: Int)
<!NO_EXPLICIT_VISIBILITY_IN_API_MODE!>inline class Value3<!>(public val inner: Int)
public inline class Value4(public val inner: Int)

/* GENERATED_FIR_TAGS: classDeclaration, primaryConstructor, propertyDeclaration */
