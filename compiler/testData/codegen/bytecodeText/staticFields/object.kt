// IGNORE_BACKEND: JVM_IR
object A {
    val r: Int = 1
}
// Field initialized in constant pool
// A super constructor call and INSTANCE put
// 2 ALOAD 0