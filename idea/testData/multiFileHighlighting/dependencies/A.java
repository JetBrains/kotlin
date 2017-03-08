package dependencies;

import sealedClass.Foo;

public class A {
    public static class B<T> {
    }

    public static B<Foo.Bar> myList = null;

    public static <T> T bar(B<T> tb) {
        return null;
    }
}
