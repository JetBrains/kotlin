object A {
    val r: Int = 1
}

// JVM_TEMPLATES
// Field initialized in constant pool
// A super constructor call and INSTANCE put
// 2 ALOAD 0

// JVM_IR_TEMPLATES
// JVM_IR generates 'dup' instead of 'astore 0; aload 0' in <clinit> method of object class
// 1 ALOAD 0