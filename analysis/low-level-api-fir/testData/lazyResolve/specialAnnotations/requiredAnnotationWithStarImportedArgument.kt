// ISSUE: KT-75844

import kotlin.annotation.AnnotationTarget.*

@Target(ANNOTATION_CLASS)
annotation class MyA<caret>nnotation
