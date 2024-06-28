// FIR_IDENTICAL
// FILE: test/NonNullApi.java
package test;

import java.lang.annotation.*;
import javax.annotation.Nonnull;
import javax.annotation.meta.TypeQualifierDefault;

@Target(ElementType.PACKAGE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Nonnull
@TypeQualifierDefault({ElementType.METHOD, ElementType.PARAMETER, ElementType.PACKAGE})
public @interface NonNullApi {
}

// FILE: test/package-info.java
@NonNullApi
package test;

// FILE: test/JpaSpecificationExecutor.java
package test;

import java.util.List;

public interface JpaSpecificationExecutor<T> {
    List<T> findAll();
}

// FILE: mockito/OngoingStubbing.java
package mockito;

public interface OngoingStubbing<T> {
    OngoingStubbing<T> thenReturn(T var1);

    public static <T> OngoingStubbing<T> when(T... methodCall) {
        return null;
    }
}


// FILE: test.kt
package test

import mockito.OngoingStubbing

fun test(wrapper: JpaSpecificationExecutor<String>, l: List<String>, l2: List<Int>) {
    OngoingStubbing.`when`(wrapper.findAll()).thenReturn(l)
    OngoingStubbing.`when`(wrapper.findAll(), l2).thenReturn(l)
}