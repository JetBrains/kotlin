package test

public class InheritMethodsDifferentReturnTypes: Object() {
    public trait Super1: Object {
        public fun foo(): CharSequence?
        public fun bar(): String?
    }

    public trait Super2: Object {
        public fun foo(): String?
        public fun bar(): CharSequence?
    }

    public trait Sub: Super1, Super2 {
    }
}
