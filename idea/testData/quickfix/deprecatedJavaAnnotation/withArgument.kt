// "Replace annotation with kotlin.annotation.Retention" "true"
// ERROR: Type mismatch: inferred type is RetentionPolicy but AnnotationRetention was expected

import java.lang.annotation.RetentionPolicy
import java.lang.annotation.Retention

@Retention<caret>(RetentionPolicy.SOURCE)
annotation class Foo