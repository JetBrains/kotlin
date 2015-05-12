package test

public class InheritMethodsDifferentReturnTypes {
    public interface Super1 {
        public fun foo(): CharSequence?
        public fun bar(): String?
    }

    public interface Super2 {
        public fun foo(): String?
        public fun bar(): CharSequence?
    }

    public interface Sub: Super1, Super2 {
    }
}
