@Target(AnnotationTarget.CLASS)
annotation class InvalidAnn

fun wrongAnnotationTargetInWhenSubject() {
    when(<!WRONG_ANNOTATION_TARGET!>@InvalidAnn<!> val x = 0) {
        0 -> {}
    }
}

annotation class ValidAnn

fun annotationInWhenSubject() {
    when(@ValidAnn val x = 0) {
        0 -> {}
    }
}
