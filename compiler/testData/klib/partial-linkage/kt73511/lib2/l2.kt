annotation class MyAnnotation

@MyAnnotationMarker(MyAnnotation::class)
data class MyAnnotationHolder(val x: Int)
