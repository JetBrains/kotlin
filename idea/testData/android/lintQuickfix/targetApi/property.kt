// INTENTION_TEXT: Add @TargetApi(LOLLIPOP) Annotation
// INSPECTION_CLASS: com.android.tools.idea.lint.AndroidLintNewApiInspection

import android.graphics.drawable.VectorDrawable

class VectorDrawableProvider {
    val VECTOR_DRAWABLE = <caret>VectorDrawable()
}