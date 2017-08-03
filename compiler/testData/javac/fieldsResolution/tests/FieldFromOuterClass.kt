// FILE: a/x.java
package a;

public class x {

    public static final int I = 42;

    public class Inner {

        public static final int I2 = I;

        public class Inner2 {
            public static final int I = x.I + I2;

            public class Inner3 extends Inner {
                public static final int CONST = I;
            }

        }

    }

    public static class Nested {
        public static final int I2 = I;

        public static class Nested2 {
            public static final int I3 = I2;
            public static final int I4 = 42;

            public class Inner {
                public static final int I5 = I4;
            }

        }

    }

}