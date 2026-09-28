// FILE: test/CallableDescriptor.java

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

// Here in 'original in emptySet<D>()' T = '@EnhancedNullability CallableDescriptor' is inferred for 'Iterable<T>.contains(T)'.
// Using value of '@EnhancedNullability CallableDescriptor' type where '@EnhancedNullability CallableDescriptor' is expected
// doesn't cause a null check.
// 0 checkExpressionValueIsNotNull
// 0 checkNotNullExpressionValue
