// "Remove EXPRESSION target" "true"
import kotlin.annotation.AnnotationTarget.*

<caret>@Retention
@Target(FIELD, EXPRESSION, PROPERTY)
annotation class Ann