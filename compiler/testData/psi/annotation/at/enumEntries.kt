enum class A {
    @[Ann] @Ann(1) X : A()

    @Ann Y : A() {}

    private @Ann() Z : A()

    @Ann @private Q

    Ann() W

    @Ann fun foo() {}
}
