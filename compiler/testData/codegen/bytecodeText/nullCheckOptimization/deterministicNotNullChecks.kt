// FILE: test/DeclarationDescriptor.java

package test;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

public interface DeclarationDescriptor {
    @NotNull
    DeclarationDescriptor getOriginal();

    @Nullable
    DeclarationDescriptor getContainingDeclaration();
}

// FILE: test/DeclarationDescriptorWithVisibility.java
package test;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

public interface DeclarationDescriptorWithVisibility extends DeclarationDescriptor {
}

// FILE: test/DeclarationDescriptorWithSource.java
package test;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

public interface DeclarationDescriptorWithSource extends DeclarationDescriptor {
    @Override
    @NotNull
    DeclarationDescriptorWithSource getOriginal();
}

// FILE: test/DeclarationDescriptorNonRoot.java
package test;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

public interface DeclarationDescriptorNonRoot extends DeclarationDescriptorWithSource {
}

// FILE: test/CallableDescriptor.java
package test;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

public interface CallableDescriptor extends DeclarationDescriptorWithVisibility, DeclarationDescriptorNonRoot {
    @NotNull
    @Override
    CallableDescriptor getOriginal();

    @NotNull
    Collection<? extends CallableDescriptor> getOverriddenDescriptors();
}

// FILE: test/k.kt
package test

fun <D : CallableDescriptor> D.overriddenTreeUniqueAsSequenceA(useOriginal: Boolean): Sequence<D> {
    val set = hashSetOf<D>()

    @Suppress("UNCHECKED_CAST")
    fun D.doBuildOverriddenTreeAsSequence(): Sequence<D> {
        return with(if (useOriginal) original as D else this) {
            if (original in set)
                emptySequence()
            else {
                emptySequence()
            }
        }
    }

    return doBuildOverriddenTreeAsSequence()
}

fun <D : CallableDescriptor> D.overriddenTreeUniqueAsSequenceB(useOriginal: Boolean): Sequence<D> {
    val set = hashSetOf<D>()

    @Suppress("UNCHECKED_CAST")
    fun D.doBuildOverriddenTreeAsSequence(): Sequence<D> {
        return with(if (useOriginal) original as D else this) {
            if (original in set)
                emptySequence()
            else {
                emptySequence()
            }
        }
    }

    return doBuildOverriddenTreeAsSequence()
}

// @KKt.class:
// 0 checkExpressionValueIsNotNull
