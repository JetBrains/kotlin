// FILE: a/X.java
package a;

public interface X {
    int I = 42;

    interface Y extends X {
        int O = I;
        class XY implements X, Y {
            public static final int I = O + X.I;
            public class XYZ extends XY implements Y {
                int O = I;
            }
        }
    }

    class C {
        public static final double Pi = 3.14;
        interface I {
            double Pi = 41.3;
            class Z {
                public static final double CONST = Pi;
            }

        }
    }
}
