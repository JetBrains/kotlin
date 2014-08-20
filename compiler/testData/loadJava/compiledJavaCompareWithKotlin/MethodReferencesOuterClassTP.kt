package test

public class MethodReferencesOuterClassTP<P>() {
    public inner class Inner() {
        public fun f<Q : P>() {}
    }
}
