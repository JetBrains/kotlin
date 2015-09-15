@file:JvmName("MultifileClass")
@file:JvmMultifileClass
<!ANNOTATION_IS_NOT_APPLICABLE_TO_MULTIFILE_CLASSES!>@file:FileAnn<!>
<!ANNOTATION_IS_NOT_APPLICABLE_TO_MULTIFILE_CLASSES!>@file:FileBinaryAnn<!>
@file:FileSourceAnn

@Target(AnnotationTarget.FILE)
@Retention(AnnotationRetention.RUNTIME)
public annotation class FileAnn

@Target(AnnotationTarget.FILE)
@Retention(AnnotationRetention.BINARY)
public annotation class FileBinaryAnn

@Target(AnnotationTarget.FILE)
@Retention(AnnotationRetention.SOURCE)
public annotation class FileSourceAnn


