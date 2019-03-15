// CONFIGURE_LIBRARY: JUnit@lib/junit-4.12.jar
import org.junit.Test

class A {
    @Test fun <caret>test_two_plus_two_equals_four() {}
}

fun test() {
    A().test_two_plus_two_equals_four()
}