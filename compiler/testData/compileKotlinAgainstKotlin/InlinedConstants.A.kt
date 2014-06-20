package constants

import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy

public val b: Byte = 100
public val s: Short = 20000
public val i: Int = 2000000
public val l: Long = 2000000000000L
public val f: Float = 3.14f
public val d: Double = 3.14
public val bb: Boolean = true
public val c: Char = '\u03c0' // pi symbol

public val str: String = ":)"

[Retention(RetentionPolicy.RUNTIME)]
public annotation class AnnotationClass(public val value: String)