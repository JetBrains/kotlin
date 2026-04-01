// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ForbidExposureOfPrivateTypesInNonPrivateInlineFunctionsInKlibs
// DIAGNOSTICS: -NOTHING_TO_INLINE

private class Private

internal inline fun isPrivate(obj: Any): String = when (obj) {
    is <!LESS_VISIBLE_TYPE_ACCESS_IN_INLINE_ERROR!>Private<!> -> "isPrivate"
    else -> "OK1"
}

internal inline fun asPrivate(obj: Any): String {
    try {
        val privateObj = obj as <!LESS_VISIBLE_TYPE_ACCESS_IN_INLINE_ERROR!>Private<!>
        return "asPrivate"
    } catch (e: ClassCastException) {
        return "OK2"
    }
}
