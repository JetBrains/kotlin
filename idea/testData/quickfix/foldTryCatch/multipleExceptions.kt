// "Lift assignment out of 'try' expression" "true"
// WITH_RUNTIME

fun foo() {
    val x: Int
    try {
        x = 1
    } catch (e: RuntimeException) {
        <caret>x = 2
    } catch (e: Exception) {
        x = 3
    } catch (e: Throwable) {
        x = 4
    }
}