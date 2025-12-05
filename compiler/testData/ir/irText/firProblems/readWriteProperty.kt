// WITH_STDLIB
// WITH_REFLECT

// SKIP_NEW_KOTLIN_REFLECT_COMPATIBILITY_CHECK
//
// To fix this test we need to migrate callables to kotlinx-metadata.
// The fix patch then should look smth like this (`kotlinFunction?.data` not being available yet):
//
//     diff --git a/core/reflection.jvm/src/kotlin/reflect/jvm/internal/KClassImpl.kt b/core/reflection.jvm/src/kotlin/reflect/jvm/internal/KClassImpl.kt
//     index 9ed68ab32888..d9e6ba610fe6 100644
//     --- a/core/reflection.jvm/src/kotlin/reflect/jvm/internal/KClassImpl.kt
//     +++ b/core/reflection.jvm/src/kotlin/reflect/jvm/internal/KClassImpl.kt
//     @@ -57,6 +57,7 @@ import kotlin.reflect.*
//      import kotlin.reflect.jvm.internal.KClassImpl.MemberBelonginess.DECLARED
//      import kotlin.reflect.jvm.internal.KClassImpl.MemberBelonginess.INHERITED
//      import kotlin.reflect.jvm.internal.types.DescriptorKType
//     +import kotlin.reflect.jvm.kotlinFunction
//      import org.jetbrains.kotlin.descriptors.ClassKind as DescriptorClassKind
//      import org.jetbrains.kotlin.descriptors.Modality as DescriptorModality
//
//     @@ -194,7 +195,9 @@ internal class KClassImpl<T : Any>(
//                  else
//                      TypeParameterTable.create(
//                          kmClass!!.typeParameters,
//     -                    (jClass.enclosingClass?.takeIf { kmClass!!.isInner }?.kotlin as? KClassImpl<*>)?.data?.value?.typeParameterTable,
//     +                    jClass.enclosingMethod?.kotlinFunction?.data?.value?.typeParameterTable
//     +                        ?: (jClass.enclosingClass?.takeIf<Class<*>> { kmClass!!.isInner }?.kotlin as? KClassImpl<*>)
//     +                            ?.data?.value?.typeParameterTable,
//                          this@KClassImpl,
//                          jClass.safeClassLoader,
//                      )
// Currently, the test fails with the following exception:
//   Caused by: kotlin.reflect.jvm.internal.KotlinReflectionInternalError: Type parameter not found: 0
//       at kotlin.reflect.jvm.internal.ConvertFromMetadataKt.toClassifier(ConvertFromMetadata.kt:179)
//       at kotlin.reflect.jvm.internal.ConvertFromMetadataKt.toKType(ConvertFromMetadata.kt:105)
//       at kotlin.reflect.jvm.internal.ConvertFromMetadataKt.toKTypeProjection(ConvertFromMetadata.kt:190)
//       at kotlin.reflect.jvm.internal.ConvertFromMetadataKt.toKType$lambda$2(ConvertFromMetadata.kt:99)
//       at kotlin.sequences.TransformingIndexedSequence$iterator$1.next(Sequences.kt:267)
//       at kotlin.sequences.SequencesKt___SequencesKt.toList(_Sequences.kt:835)
//       at kotlin.reflect.jvm.internal.ConvertFromMetadataKt.toKType(ConvertFromMetadata.kt:104)
//       at kotlin.reflect.jvm.internal.KClassImpl$Data.supertypes_delegate$lambda$0(KClassImpl.kt:272)
//       at kotlin.reflect.jvm.internal.ReflectProperties$LazySoftVal.invoke(ReflectProperties.java:70)
//       at kotlin.reflect.jvm.internal.ReflectProperties$Val.getValue(ReflectProperties.java:32)
//       at kotlin.reflect.jvm.internal.KClassImpl$Data.getSupertypes(KClassImpl.kt:255)
//       at kotlin.reflect.jvm.internal.KClassImpl.getSupertypes(KClassImpl.kt:494)
//       at kotlin.reflect.full.KClasses.getSuperclasses(KClasses.kt:191)
//       at org.jetbrains.kotlin.test.backend.handlers.RunInAlienClassLoader.dumpKClass(JvmNewKotlinReflectCompatibilityCheck.kt:187)
//       at org.jetbrains.kotlin.test.backend.handlers.RunInAlienClassLoader.dumpKClasses(JvmNewKotlinReflectCompatibilityCheck.kt:174)
//       ... 29 more

import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

class SettingType<out V : Any>(
    val type : KClass<out V>
)

class SettingReference<V : Any, T : SettingType<V>>(
    var t : T,
    var v : V
)

class IdeWizard {
    var projectTemplate by setting(SettingReference(SettingType(42::class), 42))

    private fun <V : Any, T : SettingType<V>> setting(reference: SettingReference<V, T>) =
        object : ReadWriteProperty<Any?, V?> {
            override fun setValue(thisRef: Any?, property: KProperty<*>, value: V?) {
                if (value == null) return
                reference.t = SettingType(value::class) as T
                reference.v = value
            }

            override fun getValue(thisRef: Any?, property: KProperty<*>): V? {
                return reference.v
            }
        }
}
