// WITH_STDLIB

// FILE: KotlinClass.kt

@file:OptIn(kotlin.concurrent.atomics.ExperimentalAtomicApi::class)

import kotlin.concurrent.atomics.AtomicInt

open class KotlinClass {
    open fun foo(a: AtomicInt) { }
    open val a: AtomicInt = AtomicInt(0)
}

// FILE: JavaClassWithExplicitOverride.java
import java.util.concurrent.atomic.*;

public class JavaClassWithExplicitOverride extends KotlinClass {
    @Override
    public void foo(AtomicInteger a) { }

    @Override
    public AtomicInteger getA() {
        return new AtomicInteger(1);
    }
}

// FILE: test.kt

@file:OptIn(kotlin.concurrent.atomics.ExperimentalAtomicApi::class)

import JavaClassWithExplicitOverride
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.atomics.AtomicInt

<!CONFLICTING_INHERITED_JVM_DECLARATIONS, CONFLICTING_INHERITED_JVM_DECLARATIONS!>class KotlinChildWithFakeOverride: JavaClassWithExplicitOverride()<!>

class KotlinChildWithExplicitOverride: JavaClassWithExplicitOverride() {
    <!ACCIDENTAL_OVERRIDE!>override fun foo(a: AtomicInt) {}<!>
    <!ACCIDENTAL_OVERRIDE!>override val a: AtomicInt<!> = AtomicInt(0)
}
