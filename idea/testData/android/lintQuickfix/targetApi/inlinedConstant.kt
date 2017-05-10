// INTENTION_TEXT: Add @TargetApi(KITKAT) Annotation
// INSPECTION_CLASS: org.jetbrains.android.inspections.lint.AndroidLintInspectionToolProvider$AndroidLintInlinedApiInspection

class Test {
    fun foo(): Int {
        return android.R.attr.<caret>windowTranslucentStatus
    }
}