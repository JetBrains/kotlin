// FIR_IDENTICAL
// LANGUAGE: +RepeatableAnnotations +RepeatableAnnotationContainerConstraints
// FULL_JDK

<!REPEATABLE_ANNOTATION_HAS_NESTED_CLASS_NAMED_CONTAINER_ERROR!>@Repeatable<!>
annotation class A1 {
    class Container
}


@java.lang.annotation.Repeatable(D1::class)
annotation class B1 {
    class Container
}
annotation class D1(val value: Array<B1>)

<!REDUNDANT_REPEATABLE_ANNOTATION!>@Repeatable<!>
@java.lang.annotation.Repeatable(D2::class)
annotation class B2 {
    class Container
}
annotation class D2(val value: Array<B2>)
