package test;

public class SeveralInnerClasses {

    public static class NestedFirst {
        public static class NestedSecond {
        }
    }

    public class InnerFirst {
        public class InnerSecond {
        }
    }

    public static NestedFirst.NestedSecond getNested() { return null; }

    public static InnerFirst.InnerSecond getInner() { return null; }

}
