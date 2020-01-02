@kotlin.jvm.Strictfp class A {

}

@kotlin.jvm.Strictfp object B {

}

@kotlin.jvm.Strictfp interface C {

}

fun foo() {
    @kotlin.jvm.Strictfp class D

    @kotlin.jvm.Strictfp object: Any() {}
}