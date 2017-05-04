// INTENTION_TEXT: Surround with if (VERSION.SDK_INT >= VERSION_CODES.LOLLIPOP) { ... }
// INTENTION_NOT_AVAILABLE
// INSPECTION_CLASS: org.jetbrains.android.inspections.lint.AndroidLintInspectionToolProvider$AndroidLintNewApiInspection

import android.graphics.drawable.VectorDrawable
import kotlin.reflect.KClass

annotation class SomeAnnotationWithClass(val cls: KClass<*>)

@SomeAnnotationWithClass(<caret>VectorDrawable::class)
class VectorDrawableProvider {
}