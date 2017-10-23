// INTENTION_TEXT: Surround with if (VERSION.SDK_INT >= VERSION_CODES.KITKAT) { ... }
// INSPECTION_CLASS: com.android.tools.idea.lint.AndroidLintInlinedApiInspection

class Test {
    fun foo(): Int {
        return android.R.attr.<caret>windowTranslucentStatus
    }
}