// INTENTION_TEXT: Add @RequiresApi(LOLLIPOP) Annotation
// INSPECTION_CLASS: org.jetbrains.android.inspections.lint.AndroidLintInspectionToolProvider$AndroidLintNewApiInspection
// DEPENDENCY: RequiresApi.java -> android/support/annotation/RequiresApi.java

import android.graphics.drawable.VectorDrawable

class MyVectorDrawable : <caret>VectorDrawable() {

}