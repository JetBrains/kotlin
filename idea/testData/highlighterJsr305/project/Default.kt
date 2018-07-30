import foo.A

fun main(a: A) {
    <warning descr="[RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS] Unsafe use of a nullable receiver of type String?">a.field</warning>.length
}
