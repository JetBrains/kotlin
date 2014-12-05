package test

annotation class Empty

annotation class JustAnnotation(val annotation: Empty)

annotation class AnnotationArray(val annotationArray: Array<JustAnnotation>)

JustAnnotation(Empty())
AnnotationArray(array())
class C1

AnnotationArray(array(JustAnnotation(Empty()), JustAnnotation(Empty())))
class C2
