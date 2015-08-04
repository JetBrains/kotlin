public class J {
    public class Inner {}

    public static class Nested {}

    private static class PrivateNested {}

    // This anonymous class should not appear in 'nestedClasses'
    private final Object o = new Object() {};
}
