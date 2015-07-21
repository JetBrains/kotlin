class Annotation {
    fun setProblemGroup() {}
    fun getQuickFixes() = 0
}

fun registerQuickFix(annot: Annotation) {
    annot.setProblemGroup()
    val fixes = annot.getQuickFixes()
}
