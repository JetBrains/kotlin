// VALHALLA_SUPPORT: ALL_VALUES
// LANGUAGE: +FullValueClasses

// MODULE: lib
// FILE: Point.kt
value class Point(val x: Int, val y: Int)

// MODULE: main(lib)
// FILE: User.java
public class User {
    public static String test() {
        if (!Point.class.isValue()) return "FAIL: Point.class.isValue() should be true";
        if (User.class.isValue()) return "FAIL: an identity class must not be a value class";

        Point p = new Point(1, 2);
        if (p.getX() != 1 || p.getY() != 2) return "FAIL: Point getters: " + p;

        return "OK";
    }
}

// FILE: box.kt
fun box(): String = User.test()
