import foo.A

fun main(a: A) {
    <warning descr="[NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS] Expected type does not accept nulls in Kotlin, but the value may be null in Java">a.field</warning>.length
}
