package test;

public class ReferenceCycleThroughAnnotation {
    @C(B.class)
    public class A<T extends Object> {
        public void foo() {
        }
    }

    public class B<T extends A<T>> {
    }

    public @interface C {
        public Class<?> value();
    }
}
