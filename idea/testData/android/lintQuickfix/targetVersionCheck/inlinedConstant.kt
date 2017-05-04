// INTENTION_TEXT: Surround with if (VERSION.SDK_INT >= VERSION_CODES.KITKAT) { ... }
// INSPECTION_CLASS: org.jetbrains.android.inspections.lint.AndroidLintInspectionToolProvider$AndroidLintInlinedApiInspection

class Test {
    fun foo(): Int {
        return android.R.attr.<caret>windowTranslucentStatus
    }
}