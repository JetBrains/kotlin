package test

public class InheritMethodsDifferentReturnTypes {
    public trait Super1 {
        public fun foo(): CharSequence?
        public fun bar(): String?
    }

    public trait Super2 {
        public fun foo(): String?
        public fun bar(): CharSequence?
    }

    public trait Sub: Super1, Super2 {
    }
}
