package annotations

@Target(AnnotationTarget.FILE, AnnotationTarget.FUNCTION, AnnotationTarget.TYPE, AnnotationTarget.PROPERTY_GETTER)
annotation class Simple

annotation class WithInt(val value: Int)

annotation class WithString(val s: String)

annotation class Complex(val wi: WithInt, val ws: WithString)

annotation class VeryComplex(val f: Float, val d: Double, val b: Boolean, val l: Long, val n: Int?)