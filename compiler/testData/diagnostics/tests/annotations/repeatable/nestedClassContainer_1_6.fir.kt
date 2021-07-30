// !LANGUAGE: +RepeatableAnnotations +RepeatableAnnotationContainerConstraints
// FULL_JDK

@Repeatable
annotation class A1 {
    class Container
}


@java.lang.annotation.Repeatable(D1::class)
annotation class B1 {
    class Container
}
annotation class D1(val value: Array<B1>)

@Repeatable
@java.lang.annotation.Repeatable(D2::class)
annotation class B2 {
    class Container
}
annotation class D2(val value: Array<B2>)
