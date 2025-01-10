// LANGUAGE: -JavaTypeParameterDefaultRepresentationWithDNN -AllowDnnTypeOverridingFlexibleType
// JSPECIFY_STATE: warn
// ISSUE: KT-58933
// FILE: J.java
import org.jspecify.annotations.*;

public interface J<T> {
    void simple(@NonNull T t);
}

// FILE: test.kt
<!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>class K<!><T> : J<T> {
    <!NOTHING_TO_OVERRIDE!>override<!> fun simple(t: T & Any) {}
}

class K2<T> : J<T> {
    override fun simple(t: T) {}
}
