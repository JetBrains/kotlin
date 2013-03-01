package test;

//Note: this test could be written in simple load java test, but KT-3128 prevents from writing Kotlin counterpart for it
public interface ReturnInnerSubclassOfSupersInner {
    class Super<A> {
        class Inner {
            Super<A> get() {
                throw new UnsupportedOperationException();
            }
        }
    }

    class Sub<B> extends Super<B> {
        class Inner extends Super<B>.Inner {
            Sub<B> get() {
                throw new UnsupportedOperationException();
            }
        }
    }
}
