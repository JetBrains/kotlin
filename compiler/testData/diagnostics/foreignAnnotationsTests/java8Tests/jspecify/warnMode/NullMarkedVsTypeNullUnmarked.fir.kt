// JSPECIFY_STATE: warn
// DIAGNOSTICS: -UNUSED_PARAMETER

// FILE: NullUnmarkedType.java

import org.jspecify.annotations.*;

@NullUnmarked
public interface NullUnmarkedType {

    @NullMarked
    public interface NullMarkedType {
        public String unannotatedProduce();
        public void unannotatedConsume(String arg);
    }

    public interface UnannotatedType {
        @NullMarked
        public String nullMarkedProduce();
        @NullMarked
        public void nullMarkedConsume(String arg);
    }

}

// FILE: NullUnmarkedTypeWithNullMarkedConstructor.java

import org.jspecify.annotations.*;

@NullUnmarked
public class NullUnmarkedTypeWithNullMarkedConstructor {
    @NullMarked
    public NullUnmarkedTypeWithNullMarkedConstructor(String arg) {}
}

// FILE: kotlin.kt

interface TestA: NullUnmarkedType.NullMarkedType {
    <!WRONG_TYPE_FOR_JAVA_OVERRIDE!>override<!> fun unannotatedProduce(): String?
}

interface TestB: NullUnmarkedType.UnannotatedType {
    <!WRONG_TYPE_FOR_JAVA_OVERRIDE!>override<!> fun nullMarkedProduce(): String?
}

fun test(
    a: NullUnmarkedType.NullMarkedType,
    b: NullUnmarkedType.UnannotatedType
) {
    a.unannotatedConsume(<!TYPE_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>null<!>)
    b.nullMarkedConsume(<!TYPE_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>null<!>)
    NullUnmarkedTypeWithNullMarkedConstructor(<!TYPE_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>null<!>)
}
