// INTENTION_TEXT: Surround with if (VERSION.SDK_INT >= VERSION_CODES.LOLLIPOP) { ... }
// INSPECTION_CLASS: com.android.tools.idea.lint.AndroidLintNewApiInspection

import android.app.Activity
import android.graphics.drawable.VectorDrawable

data class ValueProvider(var p1: VectorDrawable, val p2: Int)

val activity = Activity()
fun foo() {
    val (v1, v2) = ValueProvider(<caret>VectorDrawable(), 0)
}