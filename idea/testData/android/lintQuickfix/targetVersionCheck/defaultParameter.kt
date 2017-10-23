// INTENTION_TEXT: Surround with if (VERSION.SDK_INT >= VERSION_CODES.LOLLIPOP) { ... }
// INTENTION_NOT_AVAILABLE
// INSPECTION_CLASS: com.android.tools.idea.lint.AndroidLintNewApiInspection

import android.graphics.drawable.VectorDrawable


fun withDefaultParameter(vector: VectorDrawable = <caret>VectorDrawable()) {

}