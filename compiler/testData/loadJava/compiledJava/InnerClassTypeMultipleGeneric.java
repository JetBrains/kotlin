package test;
public class InnerClassTypeMultipleGeneric {
    public class BaseOuter<H1, H2> {
        abstract public class BaseInner<H3, H4> {
        }
    }

    public class Outer<E1, E2> extends BaseOuter<Integer, E1> {
        public BaseInner<Class<?>, CharSequence> bar() { return null; }
        public class Inner<E3> extends BaseOuter<E2, E3>.BaseInner<Double, String> {}
    }

    public Outer<Character, Boolean>.Inner<Byte> staticType() {
        return null;
    }
}
