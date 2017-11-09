// INTENTION_TEXT: Add @RequiresApi(M) Annotation
// INSPECTION_CLASS: org.jetbrains.android.inspections.klint.AndroidLintInspectionToolProvider$AndroidKLintNewApiInspection
// DEPENDENCY: RequiresApi.java -> android/support/annotation/RequiresApi.java
import android.app.Activity

val top: Int
    get() = Activity().<caret>checkSelfPermission(READ_CONTACTS)