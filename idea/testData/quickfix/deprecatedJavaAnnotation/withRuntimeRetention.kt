// "Replace annotation with kotlin.annotation.Retention" "true"

import java.lang.annotation.RetentionPolicy
import java.lang.annotation.Retention

@Retention<caret>(RetentionPolicy.RUNTIME)
annotation class Foo