// CONFIGURE_LIBRARY: JUnit@lib/junit-4.11.jar
// REF: (<root>).Foo
import junit.framework.TestCase

class Foo

class <caret>FooTest : TestCase()

class FooTest2 : TestCase()