// !WITH_NEW_INFERENCE
@Target(AnnotationTarget.FIELD) 
annotation class Field

@Field
annotation class Another

@Field
val x: Int = 42

@Field
val y: Int
    get() = 13

@Field
abstract class My(@Field arg: Int, @Field val w: Int) {
    @Field
    val x: Int = arg

    @Field
    val y: Int
        get() = 0

    @Field
    abstract val z: Int

    @Field
    fun foo() {}

    @Field
    val v: Int by <!UNRESOLVED_REFERENCE!>Delegates<!>.<!UNRESOLVED_REFERENCE, UNRESOLVED_REFERENCE!>lazy<!> { 42 }
}

enum class Your {
    @Field FIRST
}

interface His {
    @Field
    val x: Int

    @Field
    val y: Int
        get() = 42
}
