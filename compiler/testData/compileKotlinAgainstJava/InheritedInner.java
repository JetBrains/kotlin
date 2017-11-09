package test;

public class InheritedInner {

    public Third.Second getSecond() { return null; }

    public static class First {

        public static class Second {}

    }

    public static class Third extends First {

    } 

}
