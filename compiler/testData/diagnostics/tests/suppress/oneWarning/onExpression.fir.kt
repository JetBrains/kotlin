fun foo(): Any? {
    @Suppress("REDUNDANT_NULLABLE")
    return null as Nothing??
}