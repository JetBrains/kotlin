// SKIP_IN_FIR_TEST
package test;

class NoSamForTypeParameter {
    <K extends Runnable> void foo(K runnable1, Runnable runnable2) {}
}

class NoSamForTypeParameterDerived1 extends NoSamForTypeParameter {
    @Override
    void foo(Runnable runnable1, Runnable runnable2) {}
}

class NoSamForTypeParameterDerived2 extends NoSamForTypeParameter {
    @Override
    <K extends Runnable> void foo(K runnable1, Runnable runnable2) {}
}

class NoSamForTypeParameterDerived3 extends NoSamForTypeParameterDerived1 {
    @Override
    void foo(Runnable runnable1, Runnable runnable2) {}
}
