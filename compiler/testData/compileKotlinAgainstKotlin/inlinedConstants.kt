// IGNORE_BACKEND: JVM_IR
// TARGET_BACKEND: JVM
// FILE: A.kt

package constants

public const val b: Byte = 100
public const val s: Short = 20000
public const val i: Int = 2000000
public const val l: Long = 2000000000000L
public const val f: Float = 3.14f
public const val d: Double = 3.14
public const val bb: Boolean = true
public const val c: Char = '\u03c0' // pi symbol

public const val str: String = ":)"

@Retention(AnnotationRetention.RUNTIME)
public annotation class AnnotationClass(public val value: String)

// FILE: B.kt

import constants.*

@AnnotationClass("$b $s $i $l $f $d $bb $c $str")
class DummyClass()

fun box(): String {
    val klass = DummyClass::class.java
    val annotationClass = AnnotationClass::class.java
    val annotation = klass.getAnnotation(annotationClass)!!
    val value = annotation.value
    require(value == "100 20000 2000000 2000000000000 3.14 3.14 true \u03c0 :)", { "Annotation value: $value" })
    return "OK"
}
