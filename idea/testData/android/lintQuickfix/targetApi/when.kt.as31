// INTENTION_TEXT: Add @TargetApi(LOLLIPOP) Annotation
// INSPECTION_CLASS: com.android.tools.idea.lint.AndroidLintNewApiInspection

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