// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS

// WITH_RUNTIME

@file:StringHolder("OK")
@file:JvmName("FileClass")

@Target(AnnotationTarget.FILE)
@Retention(AnnotationRetention.RUNTIME)
public annotation class StringHolder(val value: String)

fun box(): String =
        Class.forName("FileClass").getAnnotation(StringHolder::class.java)?.value ?: "null"
