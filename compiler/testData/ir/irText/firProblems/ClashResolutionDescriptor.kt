// TARGET_BACKEND: JVM
// FULL_JDK
// WITH_STDLIB

// MUTE_SIGNATURE_COMPARISON_K2: ANY
// ^ KT-57778

import java.lang.reflect.Type

interface ComponentContainer

interface PlatformSpecificExtension<S : PlatformSpecificExtension<S>>

interface ComponentDescriptor

abstract class PlatformExtensionsClashResolver<E : PlatformSpecificExtension<E>>(
    val applicableTo: Class<E>
)

class ClashResolutionDescriptor<E : PlatformSpecificExtension<E>>(
    container: ComponentContainer,
    private val resolver: PlatformExtensionsClashResolver<E>,
    private val clashedComponents: List<ComponentDescriptor>
)

private val registrationMap = hashMapOf<Type, Any>()

fun resolveClashesIfAny(container: ComponentContainer, clashResolvers: List<PlatformExtensionsClashResolver<*>>) {
    for (resolver in clashResolvers) {
        val clashedComponents = registrationMap[resolver.applicableTo] as? Collection<ComponentDescriptor> ?: continue

        val substituteDescriptor = ClashResolutionDescriptor(container, resolver, clashedComponents.toList())
    }
}


