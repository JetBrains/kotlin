val foo: dynamic
val foo_nullable: dynamic?
val foo_a: @A dynamic
val foo_nullable_a: @A dynamic?

@Target(AnnotationTarget.TYPE, AnnotationTarget.TYPE_PARAMETER)
annotation class A