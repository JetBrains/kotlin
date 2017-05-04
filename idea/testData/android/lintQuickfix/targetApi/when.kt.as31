// INTENTION_TEXT: Add @TargetApi(LOLLIPOP) Annotation
// INSPECTION_CLASS: org.jetbrains.android.inspections.lint.AndroidLintInspectionToolProvider$AndroidLintNewApiInspection

import android.graphics.drawable.VectorDrawable

class VectorDrawableProvider {
    val flag = false
    fun getVectorDrawable(): VectorDrawable {
        return when (flag) {
            true -> <caret>VectorDrawable()
            else -> VectorDrawable()
        }
    }
}