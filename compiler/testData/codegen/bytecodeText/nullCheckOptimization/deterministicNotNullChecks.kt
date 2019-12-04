// FILE: test/CallableDescriptor.java

// IGNORE_BACKEND: JVM_IR
// ^ TODO decide if we should generate nullability assertions on arguments of 'contains' and other funs with special bridges

package test;

import org.jetbrains.annotations.NotNull;

public interface CallableDescriptor {
    @NotNull
    CallableDescriptor getOriginal();
}

// FILE: test/k.kt
package test

fun <D : CallableDescriptor> D.overriddenTreeUniqueAsSequenceA(): Boolean {
    return original in emptySet<D>()
}

// TODO: in fact, there should be an assertion, but it's missing because of https://youtrack.jetbrains.com/issue/KT-24210.
// (This test's aim is not to check whether or not the assertion is generated, but to ensure that the behavior is deterministic.)
// 0 checkExpressionValueIsNotNull
// 0 checkNotNullExpressionValue
