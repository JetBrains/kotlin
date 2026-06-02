package dependent

@Target(AnnotationTarget.TYPE)
annotation class TypeAnno(val value: String)

// The annotation application lives in the binary library, so when the type alias is expanded in
// the consumer module the resulting type annotation has no source PSI (KaAnnotation.psi == null).
typealias AnnotatedString = @TypeAnno("hello") String
