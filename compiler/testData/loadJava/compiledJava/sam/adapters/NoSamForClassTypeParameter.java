// SKIP_IN_FIR_TEST
package test;

class NoSamForTypeParameter<K extends Runnable> {
    void foo(K runnable1, Runnable runnable2) {}
}

class NoSamForTypeParameterDerived1 extends NoSamForTypeParameter<Runnable> {
    @Override
    void foo(Runnable runnable1, Runnable runnable2) {}
}

class NoSamForTypeParameterDerived2<E extends Runnable> extends NoSamForTypeParameter<E> {
     void foo(E runnable1, Runnable runnable2) {}
}

class NoSamForTypeParameterDerived3 extends NoSamForTypeParameterDerived1 {
    @Override
    void foo(Runnable runnable1, Runnable runnable2) {}
}

class NoSamForTypeParameterDerived4 extends NoSamForTypeParameterDerived2<Runnable> {
    @Override
    void foo(Runnable runnable1, Runnable runnable2) {}
}
