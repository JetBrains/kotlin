// INTENTION_TEXT: Add @RequiresApi(KITKAT) Annotation
// INSPECTION_CLASS: org.jetbrains.android.inspections.klint.AndroidLintInspectionToolProvider$AndroidKLintInlinedApiInspection
// DEPENDENCY: RequiresApi.java -> android/support/annotation/RequiresApi.java

class Test {
    fun foo(): Int {
        return android.R.attr.<caret>windowTranslucentStatus
    }
}