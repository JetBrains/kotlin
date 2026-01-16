// WITH_STDLIB
// TARGET_PLATFORM: JVM, JS

package foo

// HAS_PACKAGE: <root>
// NO_PACKAGE: bar
// HAS_PACKAGE: foo
// NO_PACKAGE: foo.bar
// HAS_PACKAGE: kotlin
// HAS_PACKAGE: kotlin.collections
// NO_PACKAGE: kotlin.coroutines.cancellation
// NO_PACKAGE: java.io