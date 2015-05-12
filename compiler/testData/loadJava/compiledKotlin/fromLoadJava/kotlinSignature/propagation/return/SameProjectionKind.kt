//ALLOW_AST_ACCESS
package test

public interface SameProjectionKind {

    public interface Super {
        public fun foo(): MutableCollection<out Number?>?

        public fun dummy() // to avoid loading as SAM interface
    }

    public interface Sub: Super {
        override fun foo(): MutableCollection<out Number?>?
    }
}
