public class AnnotationsOnNullableTypes {
    fun B<@A C?>.receiverArgument() {}

    fun parameter(a: @A C?) {}

    fun parameterArgument(a: B<@A C?>) {}

    fun returnValue(): @A C? = null

    fun <T> returnTypeParameterValue(): @A T? = null

    fun returnArgument(): B<@A C?> = null!!

    val lambdaType: @A() (() -> C)? = null

    val lambdaParameter: (@A C?) -> C = null!!

    val lambdaReturnValue: () -> @A C? = null!!

    val lambdaReceiver: (@A C?).() -> C = null!!

    val lambdaTypeWithNullableReceiver: @A C?.() -> C = null!!

    fun @receiver:A C?.functionWithAnnotatedReceiver() {}
}

@Target(AnnotationTarget.TYPE, AnnotationTarget.TYPE_PARAMETER, AnnotationTarget.VALUE_PARAMETER)
annotation class A

interface B<T>
interface C