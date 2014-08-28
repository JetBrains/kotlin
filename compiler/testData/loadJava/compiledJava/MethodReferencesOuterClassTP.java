package test;

public final class MethodReferencesOuterClassTP<P> {
    public final class Inner {
        public final <Q extends P> void f() {}
    }
}
