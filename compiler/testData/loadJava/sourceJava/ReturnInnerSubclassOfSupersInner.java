package test;

//Note: this test could be written in simple load java test, but KT-3128 prevents from writing Kotlin counterpart for it
//See the same test data in compiledJava test data.
//Test data is duplicated because Java PSI used to have some differences when loading parallel generic hierarchies from cls and source code.
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
