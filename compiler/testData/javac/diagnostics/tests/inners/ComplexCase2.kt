// FILE: p1/I.java
package p1;

public interface I {}

// FILE: p2/I.java
package p2;

public class I {}

// FILE: p/X.java
package p;

import p1.*;
import p2.I;

public class X {

    public class I1<I> {
        public I getI() { return null; }
    }

    public class N {
        public I getI() { return null; }

        public class I {}

        private class M<I, I1> {
            public I getI() { return null; }
            public I1 getI1() { return null; }

            class I1 {
                I1 getI1() { return null; }
            }
        }

    }

    public I getI() { return null; }

}