// WITH_STDLIB
// TARGET_PLATFORM: JS

package foo

// HAS_PACKAGE: <root>
// NO_PACKAGE: bar
// HAS_PACKAGE: foo
// NO_PACKAGE: foo.bar
// HAS_PACKAGE: kotlin
// HAS_PACKAGE: kotlin.collections
// HAS_PACKAGE: kotlin.reflect.js.internal
// NO_PACKAGE: java.io