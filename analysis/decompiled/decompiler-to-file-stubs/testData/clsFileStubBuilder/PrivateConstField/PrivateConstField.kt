// FIR_IDENTICAL
package test

@Retention(AnnotationRetention.RUNTIME)
annotation class A

class PrivateConstField {
    private companion object {
        const val CONST: Int = 10
        @A
        const val CONST_WITH_ANNOTATION: Int = 10

        val field: Int = 10
        @A
        val fieldWithAnnotation: Int = 10
    }
}