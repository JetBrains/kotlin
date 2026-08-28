// RUN_PIPELINE_TILL: FRONTEND

@Deprecated(message = "", ReplaceWith(""), DeprecationLevel.ERROR)
fun namedMessageThenPositional() {}

@Deprecated("", ReplaceWith(""), level = DeprecationLevel.ERROR)
fun namedLevel() {}

@Deprecated("", ReplaceWith(""), DeprecationLevel.ERROR)
fun allPositional() {}

@Deprecated(message = "", replaceWith = ReplaceWith(""), level = DeprecationLevel.ERROR)
fun allNamed() {}

@Deprecated(level = DeprecationLevel.ERROR, message = "")
fun reorderedNames() {}

fun use() {
    <!DEPRECATION_ERROR!>namedMessageThenPositional<!>()
    <!DEPRECATION_ERROR!>namedLevel<!>()
    <!DEPRECATION_ERROR!>allPositional<!>()
    <!DEPRECATION_ERROR!>allNamed<!>()
    <!DEPRECATION_ERROR!>reorderedNames<!>()
}

/* GENERATED_FIR_TAGS: functionDeclaration, stringLiteral */
