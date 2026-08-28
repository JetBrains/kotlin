// RUN_PIPELINE_TILL: FRONTEND

package myPack

val ERROR: DeprecationLevel = DeprecationLevel.WARNING

object Holder {
    val ERROR: DeprecationLevel = DeprecationLevel.WARNING
}

@Deprecated("", level = <!ANNOTATION_ARGUMENT_MUST_BE_ENUM_CONST!>ERROR<!>)
fun shadowedByTopLevelProperty() {}

@Deprecated("", level = <!ANNOTATION_ARGUMENT_MUST_BE_ENUM_CONST!>Holder.ERROR<!>)
fun shadowedByObjectProperty() {}

fun use() {
    <!DEPRECATION_ERROR!>shadowedByTopLevelProperty<!>()
    <!DEPRECATION_ERROR!>shadowedByObjectProperty<!>()
}

/* GENERATED_FIR_TAGS: functionDeclaration, objectDeclaration, propertyDeclaration, stringLiteral */
