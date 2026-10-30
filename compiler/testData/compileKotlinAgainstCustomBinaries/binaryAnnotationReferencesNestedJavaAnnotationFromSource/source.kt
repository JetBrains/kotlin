import p.Outer
import p.Ref

fun annotationValue(): Int = Ref::class.java.getAnnotation(Outer.Ann::class.java).value
