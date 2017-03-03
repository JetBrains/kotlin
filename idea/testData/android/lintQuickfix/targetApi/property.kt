// INTENTION_TEXT: Add @TargetApi(LOLLIPOP) Annotation
// INSPECTION_CLASS: org.jetbrains.android.inspections.klint.AndroidLintInspectionToolProvider$AndroidKLintNewApiInspection

import android.graphics.drawable.VectorDrawable

class VectorDrawableProvider {
    val VECTOR_DRAWABLE = <caret>VectorDrawable()
}