package test

public class MethodReferencesOuterClassTP<P>() : java.lang.Object() {
    public class Inner() : java.lang.Object() {
        public fun f<Q : P>() {}
    }
}
