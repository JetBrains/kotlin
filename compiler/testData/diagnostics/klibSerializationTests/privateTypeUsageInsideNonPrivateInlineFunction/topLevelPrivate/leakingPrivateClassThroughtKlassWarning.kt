// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: -ForbidExposureOfPrivateTypesInNonPrivateInlineFunctionsInKlibs
// DIAGNOSTICS: -NOTHING_TO_INLINE

private class Private

internal inline fun getPrivateKlass(): String {
    val klass = <!LESS_VISIBLE_TYPE_ACCESS_IN_INLINE_ERROR!>Private<!>::class
    return klass.<!LESS_VISIBLE_TYPE_IN_INLINE_ACCESSED_SIGNATURE_ERROR!>simpleName<!> ?: "null"
}
