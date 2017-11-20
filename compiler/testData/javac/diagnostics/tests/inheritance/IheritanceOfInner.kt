// FILE: a/d.java
package a;

public class d<T> {

    public static class Inner<X> extends d<Integer> {

        public class Y<C> extends d.Inner<String> {

            public class Z<Z> extends d.Inner<String>.Y<Integer> {

                public class N<N> extends d.Inner<Integer>.Y<Double>.Z<Integer> {}
            }

        }

    }

}