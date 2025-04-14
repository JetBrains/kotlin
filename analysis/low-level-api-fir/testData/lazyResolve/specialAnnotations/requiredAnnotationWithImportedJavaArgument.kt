// SKIP_WHEN_OUT_OF_CONTENT_ROOT
// ISSUE: KT-75844

import java.lang.annotation.ElementType.ANNOTATION_TYPE

@java.lang.annotation.Target(ANNOTATION_TYPE)
annotation class MyAnnot<caret>ation
