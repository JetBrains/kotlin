// !WITH_NEW_INFERENCE

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
    override val <!PROPERTY_TYPE_MISMATCH_ON_OVERRIDE!>annotationsByFqNameHash<!> by buildLazyValueForMap()
}
