package test

public class InheritMethodsDifferentReturnTypesGeneric {
    public trait Super1<F, B> {
        public fun foo(): F?
        public fun bar(): B?
    }

    public trait Super2<FF, BB> {
        public fun foo(): FF?
        public fun bar(): BB?
    }

    public trait Sub: Super1<String, CharSequence>, Super2<CharSequence, String> {
    }
}
