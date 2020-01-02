// !DIAGNOSTICS: -UNUSED_VARIABLE
class Annotation {
    fun setProblemGroup() {}
    fun getQuickFixes() = 0
}

fun registerQuickFix(annotation: Annotation) {
    annotation.setProblemGroup()
    val fixes = annotation.getQuickFixes()
}
