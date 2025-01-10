// LANGUAGE: -JavaTypeParameterDefaultRepresentationWithDNN +AllowDnnTypeOverridingFlexibleType
// JSPECIFY_STATE: warn
// ISSUE: KT-58933
// FILE: J.java
import org.jspecify.annotations.*;

public interface J<T> {
    void simple(@NonNull T t);
}

// FILE: test.kt
class K<T> : J<T> {
    override fun simple(t: T & Any) {}
}

class K2<T> : J<T> {
    <!WRONG_NULLABILITY_FOR_JAVA_OVERRIDE!>override<!> fun simple(t: T) {}
}
