/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.reflect.jvm.internal

import kotlin.LazyThreadSafetyMode.PUBLICATION
import kotlin.jvm.internal.CallableReference
import kotlin.metadata.KmProperty
import kotlin.reflect.KMutableProperty

internal open class KotlinKPropertyN<out V>(
    container: KDeclarationContainerImpl, signature: String, rawBoundReceiver: Any?, kmProperty: KmProperty,
    overriddenStorage: KCallableOverriddenStorage,
) : KotlinKProperty<V>(container, signature, rawBoundReceiver, kmProperty, overriddenStorage) {
    override val getter: Getter<V> by lazy(PUBLICATION) { Getter(this) }

    override fun shallowCopy(container: KDeclarationContainerImpl, overriddenStorage: KCallableOverriddenStorage): ReflectKCallable<V> =
        KotlinKPropertyN(container, signature, CallableReference.NO_RECEIVER, kmProperty, overriddenStorage)

    override fun rebindSameArity(boundReceiver: Any?): ReflectKProperty<V> =
        KotlinKPropertyN(container, signature, boundReceiver, kmProperty, overriddenStorage)

    override fun unbindToHigherArity(): ReflectKProperty<V> =
        throw KotlinReflectionInternalError("Cannot unbind KPropertyN: $this")

    override fun bindToLowerArity(boundReceiver: Any?): ReflectKProperty<V> =
        throw KotlinReflectionInternalError("Cannot bind KPropertyN: $this")

    class Getter<out V>(override val property: KotlinKPropertyN<V>) : KotlinKProperty.Getter<V>()
}

internal class KotlinKMutablePropertyN<V>(
    container: KDeclarationContainerImpl, signature: String, rawBoundReceiver: Any?, kmProperty: KmProperty,
    overriddenStorage: KCallableOverriddenStorage,
) : KotlinKPropertyN<V>(container, signature, rawBoundReceiver, kmProperty, overriddenStorage), KMutableProperty<V> {
    override val setter: Setter<V> by lazy(PUBLICATION) { Setter(this) }

    override fun shallowCopy(container: KDeclarationContainerImpl, overriddenStorage: KCallableOverriddenStorage): ReflectKCallable<V> =
        KotlinKMutablePropertyN(container, signature, CallableReference.NO_RECEIVER, kmProperty, overriddenStorage)

    override fun rebindSameArity(boundReceiver: Any?): ReflectKProperty<V> =
        KotlinKMutablePropertyN(container, signature, boundReceiver, kmProperty, overriddenStorage)

    class Setter<V>(override val property: KotlinKMutablePropertyN<V>) : KotlinKProperty.Setter<V>()
}
