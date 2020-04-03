// FILE: Descriptor.java
// FULL_JDK

public interface Descriptor

// FILE: ResolvedCall.java

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface ResolvedCall<D extends Descriptor> {
    @NotNull
    D getResultingDescriptor();

    @Nullable
    D getCandidateDescriptor();
}

// FILE: test.kt

val Descriptor.name = "123"
fun Descriptor.correct(): Boolean = true
fun Descriptor.foo() {}

interface Call<D : Descriptor> {
    val resultingDescriptor: D
}

fun <D : Descriptor> test(call: Call<D>, resolvedCall: ResolvedCall<D>) {
    call.resultingDescriptor.name
    resolvedCall.resultingDescriptor.name

    val resolvedDescriptor = resolvedCall.candidateDescriptor
    if (resolvedDescriptor?.correct() != true) return
    resolvedDescriptor.foo()
}

fun otherTest(call: Call<*>, resolvedCall: ResolvedCall<*>) {
    call.resultingDescriptor.name
    resolvedCall.resultingDescriptor.name
}