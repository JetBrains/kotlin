// INTENTION_TEXT: Add @RequiresApi(LOLLIPOP) Annotation
// INSPECTION_CLASS: org.jetbrains.android.inspections.klint.AndroidLintInspectionToolProvider$AndroidKLintNewApiInspection
// DEPENDENCY: RequiresApi.java -> android/support/annotation/RequiresApi.java

import android.graphics.drawable.VectorDrawable

class VectorDrawableProvider {
    fun getVectorDrawable(): VectorDrawable {
        with(this) {
            return <caret>VectorDrawable()
        }
    }
}