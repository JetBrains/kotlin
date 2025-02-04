// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// JDK_KIND: FULL_JDK_11
// JVM_TARGET: 10
// KOTLINC_ARGS: -Xjdk-release=9
// JDK_RELEASE: 9
package foo

class Foo {
    val z: java.nio.ByteBuffer? = null
}
