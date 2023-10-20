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

public actual abstract <!ACTUAL_CLASSIFIER_MUST_HAVE_THE_SAME_SUPERTYPES_AS_NON_FINAL_EXPECT_CLASSIFIER_WARNING!>class AbstractMutableMap<!><K, V>() : MutableMap<K, V>, AbstractMap<K, V>()
