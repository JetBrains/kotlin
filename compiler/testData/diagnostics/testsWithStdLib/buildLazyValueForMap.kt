// FIR_IDENTICAL

interface ClassId

interface JavaAnnotation {
    val classId: ClassId?
}

interface JavaAnnotationOwner {
    val annotations: Collection<JavaAnnotation>
}

interface MapBasedJavaAnnotationOwner : JavaAnnotationOwner {
    val annotationsByFqNameHash: Map<Int?, JavaAnnotation>
}

fun JavaAnnotationOwner.buildLazyValueForMap() = lazy {
    annotations.associateBy { it.classId?.hashCode() }
}

abstract class BinaryJavaMethodBase(): MapBasedJavaAnnotationOwner {
    override val annotationsByFqNameHash by buildLazyValueForMap()
}
