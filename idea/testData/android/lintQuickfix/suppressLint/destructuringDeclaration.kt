// INTENTION_TEXT: Suppress: Add @SuppressLint("SdCardPath") annotation
// INSPECTION_CLASS: org.jetbrains.android.inspections.lint.AndroidLintInspectionToolProvider$AndroidLintSdCardPathInspection

fun foo() {
    val (a: String, b: String) = "<caret>/sdcard"
}

operator fun CharSequence.component1(): String = "component1"
operator fun CharSequence.component2(): String = "component2"
