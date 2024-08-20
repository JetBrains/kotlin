// SKIP_WHEN_OUT_OF_CONTENT_ROOT
// WITH_FIR_TEST_COMPILER_PLUGIN
package test

import test.FirstTarget.*

@NestedAnnotation
annotation class OuterAnnotation

@org.jetbrains.kotlin.fir.plugin.MySerializable
@OuterAnnotation
class FirstTarget {

    annotation class NestedAnnotation

}

@org.jetbrains.kotlin.fir.plugin.CoreSerializer
class Serializer

val generatedMethodReference = Serializer::serialize<caret>FirstTarget
