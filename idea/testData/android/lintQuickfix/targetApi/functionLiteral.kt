// INTENTION_TEXT: Add @TargetApi(LOLLIPOP) Annotation
// INSPECTION_CLASS: org.jetbrains.android.inspections.klint.AndroidLintInspectionToolProvider$AndroidKLintNewApiInspection

import android.graphics.drawable.VectorDrawable

class VectorDrawableProvider {
    fun getVectorDrawable(): VectorDrawable {
        with(this) {
            return <caret>VectorDrawable()
        }
    }
}