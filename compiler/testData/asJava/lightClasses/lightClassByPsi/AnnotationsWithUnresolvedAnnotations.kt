@UnresolvedAnnotation1 @Retention(AnnotationRetention.SOURCE) @Target(allowedTargets = [AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.EXPRESSION])
annotation class Anno1

@Retention(AnnotationRetention.SOURCE) @UnresolvedAnnotation1  @Target(allowedTargets = [AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.EXPRESSION])
annotation class Anno2

@Retention(AnnotationRetention.SOURCE) @Target(allowedTargets = [AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.EXPRESSION]) @UnresolvedAnnotation1
annotation class Anno3

@UnresolvedAnnotation1 @Retention(AnnotationRetention.SOURCE) @UnresolvedAnnotation2 @Target(allowedTargets = [AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.EXPRESSION])
annotation class Anno4

// COMPILATION_ERRORS