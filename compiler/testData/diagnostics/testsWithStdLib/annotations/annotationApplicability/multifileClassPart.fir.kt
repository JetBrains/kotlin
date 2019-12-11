@file:JvmName("MultifileClass")
@file:JvmMultifileClass
@file:FileAnn
@file:FileBinaryAnn
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


