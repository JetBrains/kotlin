// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ExplicitInlineValueClassesSyntaxRequiredOnAllPlatforms
// RENDER_DIAGNOSTICS_FULL_TEXT

<!INLINE_VALUE_CLASS_WITHOUT_EXPLICIT_INLINE_ERROR!>value<!> class IC(val x: Int)

<!UNSUPPORTED_FEATURE!>value<!> class IC2(val x: Int, val y: Int)

inline value class CorrectIC(val x: Int)
