package a

@Retention(AnnotationRetention.RUNTIME)
annotation class Ann

interface Tr {
    @Ann
    fun foo() {}
}