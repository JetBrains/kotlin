package test

public class InheritMethodsDifferentReturnTypesGeneric: Object() {
    public trait Super1<F, B>: Object {
        public fun foo(): F?
        public fun bar(): B?
    }

    public trait Super2<FF, BB>: Object {
        public fun foo(): FF?
        public fun bar(): BB?
    }

    public trait Sub: Super1<String, CharSequence>, Super2<CharSequence, String> {
    }
}
