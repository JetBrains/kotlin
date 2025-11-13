// TARGET_BACKEND: JVM_IR
// WITH_STDLIB
// FULL_JDK
// java.lang.NoSuchMethodError: No interface method getAnnotation(Ljava/lang/Class;)Ljava/lang/annotation/Annotation; in class Ljava/lang/reflect/TypeVariable; or its super classes (declaration of 'java.lang.reflect.TypeVariable' appears in /system/framework/core-oj.jar)
// IGNORE_BACKEND: ANDROID

@Target(AnnotationTarget.TYPE_PARAMETER)
annotation class Ann(val value: String)

class C<T, @Ann("OK") U>

fun box(): String =
    C::class.java.typeParameters[1].getAnnotation(Ann::class.java).value
