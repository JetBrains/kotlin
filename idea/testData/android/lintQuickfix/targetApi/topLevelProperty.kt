// INTENTION_TEXT: Add @TargetApi(M) Annotation
// INSPECTION_CLASS: org.jetbrains.android.inspections.klint.AndroidLintInspectionToolProvider$AndroidKLintNewApiInspection
import android.app.Activity

val top: Int
    get() = Activity().<caret>checkSelfPermission(READ_CONTACTS)