// INTENTION_TEXT: Surround with if (VERSION.SDK_INT >= VERSION_CODES.LOLLIPOP) { ... }
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