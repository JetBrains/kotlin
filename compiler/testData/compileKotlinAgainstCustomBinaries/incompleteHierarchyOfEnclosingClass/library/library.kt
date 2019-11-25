package test

public open class Super

public class SubClass : Super() {
    inner class Inner
    class InnerStatic
}

public object SubObject : Super() {
    class InnerStatic
}
