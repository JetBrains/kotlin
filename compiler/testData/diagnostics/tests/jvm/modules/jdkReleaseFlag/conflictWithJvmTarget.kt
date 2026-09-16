// RUN_PIPELINE_TILL: CODEGEN
// JDK_KIND: FULL_JDK_11
// JVM_TARGET: 10
// JDK_RELEASE: 9
package foo

class Foo {
    val z: java.nio.ByteBuffer? = null
}

/* GENERATED_FIR_TAGS: classDeclaration, nullableType, propertyDeclaration */
