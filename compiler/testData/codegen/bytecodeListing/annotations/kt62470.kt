@Retention(AnnotationRetention.RUNTIME)
annotation class First

enum class E {
    @First
    E1 {
        fun foo() = "something"
    }
}