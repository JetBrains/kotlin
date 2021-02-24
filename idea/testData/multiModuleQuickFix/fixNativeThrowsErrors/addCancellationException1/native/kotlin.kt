package kotlin

import kotlin.reflect.KClass

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CONSTRUCTOR)
@Retention(AnnotationRetention.SOURCE)
public annotation class Throws(vararg val exceptionClasses: KClass<out Throwable>)

public open class Exception : Throwable()

public open class RuntimeException : Exception()

public open class IllegalStateException : RuntimeException()

public open class Error : Throwable()
