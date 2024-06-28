// FIR_IDENTICAL

// FILE: test.kt

// We inherit getReturnType
// from CallableDescriptor with return type String? and
// from ClassConstructorDescriptorImpl with return type String
// but because ClassConstructorDescriptorImpl.getReturnType subsumes CallableDescriptor.getReturnType, we don't report RETURN_TYPE_MISMATCH_ON_INHERITANCE.
class DeserializedClassConstructorDescriptor : CallableDescriptor, ClassConstructorDescriptorImpl()

// FILE: ClassConstructorDescriptorImpl.java

// IJ reports an inspection warning
// "Non-annotated method 'getReturnType' from 'FunctionDescriptorImpl' implements non-null method from 'ConstructorDescriptor'"
// which is the underlying issue.
public class ClassConstructorDescriptorImpl extends FunctionDescriptorImpl implements ConstructorDescriptor {}

// FILE: FunctionDescriptorImpl.java
public abstract class FunctionDescriptorImpl implements CallableDescriptor {
    @Override
    public String getReturnType() {
        return null;
    }
}

// FILE: ConstructorDescriptor.java
import org.jetbrains.annotations.NotNull;

public interface ConstructorDescriptor extends CallableDescriptor {
    @NotNull
    @Override
    String getReturnType();
}

// FILE: CallableDescriptor.java
import org.jetbrains.annotations.Nullable;

public interface CallableDescriptor {
    @Nullable
    String getReturnType();
}