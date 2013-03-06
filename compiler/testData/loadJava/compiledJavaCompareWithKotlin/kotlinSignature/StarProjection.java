package test;

import jet.runtime.typeinfo.KotlinSignature;

public final class StarProjection {
    @KotlinSignature("fun foo(): MyClass<*>")
    public final MyClass<?> foo() {
        throw new UnsupportedOperationException();
    }

    public interface MyClass<T extends CharSequence> {
    }
}
