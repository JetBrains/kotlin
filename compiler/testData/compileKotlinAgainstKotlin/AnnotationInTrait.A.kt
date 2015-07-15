package a

annotation(retention = AnnotationRetention.RUNTIME) class Ann

interface Tr {
    Ann
    fun foo() {}
}