enum class A {
    @[Ann] @Ann(1) X(),

    @Ann Y() {},

    private @Ann() Z(),

    @Ann @private Q,

    // TODO: try to make Ann() working here (?)
    @Ann() W;

    @Ann fun foo() {}
}
