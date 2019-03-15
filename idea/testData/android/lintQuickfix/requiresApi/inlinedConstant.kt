// INTENTION_TEXT: Add @RequiresApi(KITKAT) Annotation
// INSPECTION_CLASS: com.android.tools.idea.lint.AndroidLintInlinedApiInspection
// DEPENDENCY: RequiresApi.java -> android/support/annotation/RequiresApi.java

class Test {
    fun foo(): Int {
        return android.R.attr.<caret>windowTranslucentStatus
    }
}