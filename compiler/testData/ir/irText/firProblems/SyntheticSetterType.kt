// FIR_IDENTICAL
// TARGET_BACKEND: JVM
// WITH_STDLIB
// FILE: PropertyDescriptorImpl.java

import org.jetbrains.annotations.NotNull;
import java.util.*;

public class PropertyDescriptorImpl implements PropertyDescriptor {
    @Override
    public void setOverriddenDescriptors(@NotNull Collection<? extends CallableMemberDescriptor> overriddenDescriptors) {
        this.overriddenProperties = (Collection<? extends PropertyDescriptor>) overriddenDescriptors;
    }

    @NotNull
    @Override
    public Collection<? extends PropertyDescriptor> getOverriddenDescriptors() {
        return overriddenProperties != null ? overriddenProperties : Collections.<PropertyDescriptor>emptyList();
    }
}

// FILE: PropertyDescriptor.java

import org.jetbrains.annotations.NotNull;
import java.util.*;

public interface PropertyDescriptor extends CallableMemberDescriptor {
    @NotNull
    @Override
    Collection<? extends PropertyDescriptor> getOverriddenDescriptors();
}

// FILE: CallableMemberDescriptor.java

import org.jetbrains.annotations.NotNull;
import java.util.*;

public interface CallableMemberDescriptor {
    @NotNull
    Collection<? extends CallableMemberDescriptor> getOverriddenDescriptors();

    void setOverriddenDescriptors(@NotNull Collection<? extends CallableMemberDescriptor> overriddenDescriptors);
}

// FILE: SyntheticSetterType.kt

fun foo(descriptor: PropertyDescriptorImpl) {
    descriptor.overriddenDescriptors = emptyList()
}