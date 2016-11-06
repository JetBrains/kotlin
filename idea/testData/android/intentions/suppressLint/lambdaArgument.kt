// INTENTION_TEXT: Suppress: Add @SuppressLint("SdCardPath") annotation
// INSPECTION_CLASS: org.jetbrains.android.inspections.klint.AndroidLintInspectionToolProvider$AndroidKLintSdCardPathInspection

fun foo(l: Any) = l

fun bar() {
    foo() {
        "<caret>/sdcard"
    }
}