/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.reflect.jvm.internal

import kotlin.LazyThreadSafetyMode.PUBLICATION
import kotlin.metadata.KmProperty
import kotlin.reflect.KMutableProperty

internal open class KotlinKPropertyN<out V>(
    container: KDeclarationContainerImpl, signature: String, rawBoundReceiver: Any?, kmProperty: KmProperty,
) : KotlinKProperty<V>(container, signature, rawBoundReceiver, kmProperty) {
    override val getter: Getter<V> by lazy(PUBLICATION) { Getter(this) }

    class Getter<out V>(override val property: KotlinKPropertyN<V>) : KotlinKProperty.Getter<V>()
}

internal class KotlinKMutablePropertyN<V>(
    container: KDeclarationContainerImpl, signature: String, rawBoundReceiver: Any?, kmProperty: KmProperty,
) : KotlinKPropertyN<V>(container, signature, rawBoundReceiver, kmProperty), KMutableProperty<V> {
    override val setter: Setter<V> by lazy(PUBLICATION) { Setter(this) }

    class Setter<V>(override val property: KotlinKMutablePropertyN<V>) : KotlinKProperty.Setter<V>()
}
