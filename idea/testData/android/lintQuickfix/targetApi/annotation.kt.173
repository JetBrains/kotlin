// INTENTION_TEXT: Add @TargetApi(LOLLIPOP) Annotation
// INSPECTION_CLASS: org.jetbrains.android.inspections.klint.AndroidLintInspectionToolProvider$AndroidKLintNewApiInspection

import android.graphics.drawable.VectorDrawable
import kotlin.reflect.KClass

annotation class SomeAnnotationWithClass(val cls: KClass<*>)

@SomeAnnotationWithClass(<caret>VectorDrawable::class)
class VectorDrawableProvider {
}