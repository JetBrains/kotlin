// Class constructor parameter type CAN be recursively annotated
target(AnnotationTarget.TYPE)
annotation class RecursivelyAnnotated(val x: @RecursivelyAnnotated(1) Int)