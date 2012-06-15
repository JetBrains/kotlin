package test;

public class MethodTypePTwoUpperBounds {
    public <T extends Cloneable & Runnable> void foo() {}
}
