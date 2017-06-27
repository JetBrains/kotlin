// FILE: p/J.java
package p;

public class J {

    public interface Z {}

    public class I {}

    public class T {}

}

// FILE: p/D.java
package p;

public class D<Z> extends J {

    public Z getZ() { return null; }
    public <Z> Z getZ2(Z z) { return z; }
    public I getI() { return null; }

    public class Z<I> {
        public Z getZ() { return null; }
        public I getI() { return null; }
    }

    public class O {
        public class Z {}
        public Z getZ() { return null; }
        public <Z> Z getZ2() { return null; }
    }

    public class Test<Z, T> {

        public Z getZ() { return null; }

        public class Inner<I, T> {
            public Z getZ() { return null; }
            public T getT() { return null; }
            public I getI() { return null; }
        }

    }

}

// FILE: test.kt
package p

fun test() = D<String>().getZ2(1)