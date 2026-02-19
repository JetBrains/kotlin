fun box(): String {
    try {
        null as Nothing
    } catch (x: ClassCastException) {
        return "OK"
    } catch (x: NullPointerException) {
        return "OK"
    }
    return "Fail"
}
