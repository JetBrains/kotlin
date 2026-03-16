fun box(): String {
    try {
        @Suppress("CAST_NEVER_SUCCEEDS_ERROR")
        null as Nothing
    } catch (x: ClassCastException) {
        return "OK"
    } catch (x: NullPointerException) {
        return "OK"
    }
    return "Fail"
}
