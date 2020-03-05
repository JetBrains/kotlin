// FILE: fakeOverridesForJavaStaticMembers.kt
import a.Base

class Test : Base()

// FILE: a/Base.java
package a

public class Base {
    public static void publicStaticMethod() {}
    protected static void protectedStaticMethod() {}
    static void packagePrivateStaticMethod() {}
    private static void privateStaticMethod() {}
}