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
