package dependency

public open class A() {
    public open fun f() {}
}

public open class B(): A() {
}