// FIR_IDENTICAL
// FILE: exp/Ns.java
package exp;

public final class Ns {

    public static class Element<E extends Element<E>> {}

    public static class Foo<T extends Element<T>> {
        public Bar<T> getBar() {
            return new Bar<>();
        }
    }

    public static class Bar<U extends Element<U>> {
        public String getName() {
            return "";
        }
    }
}

// FILE: exp/main.kt
package exp

val Ns.Foo<*>.name
    get() = this.bar.name
