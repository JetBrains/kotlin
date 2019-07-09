// CONFIGURE_LIBRARY: JUnit@lib/junit-4.12.jar
// IS_APPLICABLE: false

import org.junit.Test

class A {
    @Test fun <caret>testTwoPlusTwoEqualsFour() {}
}