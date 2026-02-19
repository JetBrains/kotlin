// SKIP_WHEN_OUT_OF_CONTENT_ROOT
// ISSUE: KT-75844

import java.lang.annotation.ElementType.*

@java.lang.annotation.Target(ANNOTATION_TYPE)
annotation class MyAnno<caret>tation
