import kotlin.reflect.KClass

@Retention(AnnotationRetention.RUNTIME)
annotation class Ann(val arg: KClass<*>)

class OK

@Ann(OK::class) class MyClass

fun test(): String {
    val arg = MyClass::class.java.getAnnotation(Ann::class.java).arg.java
    return arg.getSimpleName()
}

// 0 INVOKESTATIC kotlin/jvm/internal/Reflection\.getOrCreateKotlinClass
// 0 INVOKESTATIC kotlin/jvm/JvmClassMappingKt\.getJavaClass
