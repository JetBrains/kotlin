// LANGUAGE: +DefinitelyNonNullableTypes
// SKIP_KT_DUMP

// Exception in new-reflect implementation:
// Caused by: kotlin.reflect.jvm.internal.KotlinReflectionInternalError: Type parameter not found: 0
//     at kotlin.reflect.jvm.internal.ConvertFromMetadataKt.toClassifier(ConvertFromMetadata.kt:179)
//     at kotlin.reflect.jvm.internal.ConvertFromMetadataKt.toKType(ConvertFromMetadata.kt:105)
//     at kotlin.reflect.jvm.internal.ConvertFromMetadataKt.toKTypeProjection(ConvertFromMetadata.kt:190)
//     at kotlin.reflect.jvm.internal.ConvertFromMetadataKt.toKType$lambda$2(ConvertFromMetadata.kt:99)
//     at kotlin.sequences.TransformingIndexedSequence$iterator$1.next(Sequences.kt:267)
//     at kotlin.sequences.SequencesKt___SequencesKt.toList(_Sequences.kt:830)
//     at kotlin.reflect.jvm.internal.ConvertFromMetadataKt.toKType(ConvertFromMetadata.kt:104)
//     at kotlin.reflect.jvm.internal.KClassImpl$Data.supertypes_delegate$lambda$0(KClassImpl.kt:272)
//     at kotlin.reflect.jvm.internal.ReflectProperties$LazySoftVal.invoke(ReflectProperties.java:70)
//     at kotlin.reflect.jvm.internal.ReflectProperties$Val.getValue(ReflectProperties.java:32)
//     at kotlin.reflect.jvm.internal.KClassImpl$Data.getSupertypes(KClassImpl.kt:255)
//     at kotlin.reflect.jvm.internal.KClassImpl.getSupertypes(KClassImpl.kt:494)
//     at kotlin.reflect.full.KClasses.getSuperclasses(KClasses.kt:191)
//     at org.jetbrains.kotlin.test.backend.handlers.RunInAlienClassLoader.dumpKClass(JvmNewKotlinReflectCompatibilityCheck.kt:187)
//     at org.jetbrains.kotlin.test.backend.handlers.RunInAlienClassLoader.dumpKClasses(JvmNewKotlinReflectCompatibilityCheck.kt:174)
//     ... 25 more
// SKIP_NEW_KOTLIN_REFLECT_COMPATIBILITY_CHECK

fun interface FIn<in T> {
    fun f(x: T)
}

class Test<S> {
    fun foo() = FIn<S & Any> { sx -> sx.toString() }
}

fun <T> bar() {
    object : FIn<T & Any> {
        override fun f(sx: (T & Any)) { sx.toString() }
    }
}

interface I1<in T> {
   val l: T.() -> Unit
}

interface I2<in T> {
    val sam: FIn<T>
}

abstract class AC<T> : I1<T>, I2<T> {
    override val sam: FIn<T> = FIn(l)
}

abstract class AD<T> : AC<T & Any>() {
    override val l: (T & Any).() -> Unit = { }
}