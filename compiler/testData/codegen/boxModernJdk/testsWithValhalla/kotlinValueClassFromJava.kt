// VALHALLA_SUPPORT: ALL_VALUES
// LANGUAGE: +FullValueClasses

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
value class Point(val x: Int, val y: Int)

fun box(): String = User.test()
