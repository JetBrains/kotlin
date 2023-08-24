// WITH_STDLIB
// MODULE: m1-common
// FILE: common.kt

// K2: false positve INCOMPATIBLE_MATCHING: KT-60155
public expect abstract class AbstractMutableMap<K, V> : MutableMap<K, V> {
    override val values: MutableCollection<V>
}

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

import java.util.AbstractMap

@OptIn(kotlin.ExperimentalMultiplatform::class)
@kotlin.AllowDifferentMembersInActual
public actual abstract class AbstractMutableMap<K, V>() : MutableMap<K, V>, AbstractMap<K, V>()
