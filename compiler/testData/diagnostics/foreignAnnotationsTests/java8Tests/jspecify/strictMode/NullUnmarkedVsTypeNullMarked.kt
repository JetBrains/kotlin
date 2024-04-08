// FIR_IDENTICAL
// JSPECIFY_STATE: strict
// DIAGNOSTICS: -UNUSED_PARAMETER

// FILE: NullMarkedType.java

import org.jspecify.annotations.*;

@NullMarked
public interface NullMarkedType {

    @NullUnmarked
    public interface NullUnmarkedType {
        public String unannotatedProduce();
        public void unannotatedConsume(String arg);
    }

    public interface UnannotatedType {
        @NullUnmarked
        public String nullUnmarkedProduce();
        @NullUnmarked
        public void nullUnmarkedConsume(String arg);
    }

}

// FILE: NullMarkedTypeWithNullUnmarkedConstructor.java

import org.jspecify.annotations.*;

@NullMarked
public class NullMarkedTypeWithNullUnmarkedConstructor {
    @NullUnmarked
    public NullMarkedTypeWithNullUnmarkedConstructor(String arg) {}
}

// FILE: kotlin.kt

interface TestA: NullMarkedType.NullUnmarkedType {
    override fun unannotatedProduce(): String?
}

interface TestB: NullMarkedType.UnannotatedType {
    override fun nullUnmarkedProduce(): String?
}

fun test(
    a: NullMarkedType.NullUnmarkedType,
    b: NullMarkedType.UnannotatedType
) {
    a.unannotatedConsume(null)
    b.nullUnmarkedConsume(null)
    NullMarkedTypeWithNullUnmarkedConstructor(null)
}
