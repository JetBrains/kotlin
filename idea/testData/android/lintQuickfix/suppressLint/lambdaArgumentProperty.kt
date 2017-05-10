// INTENTION_TEXT: Suppress: Add @SuppressLint("SdCardPath") annotation
// INSPECTION_CLASS: org.jetbrains.android.inspections.lint.AndroidLintInspectionToolProvider$AndroidLintSdCardPathInspection

fun foo(l: Any) = l

val bar = foo() { "<caret>/sdcard" }