fun f(): Boolean = "non-primitive" == null
fun g(): Boolean = null == "non-primitive"
fun h(): Boolean = "non-primitive".equals(null)
//fun i(): Boolean = null.equals("non-primitive")
//See KT-33757

// JVM does not optimize h() to constant false

// 0 IF

// JVM_TEMPLATES
// 2 ICONST_0
// 1 ACONST_NULL
// 1 INVOKEVIRTUAL

// JVM_IR_TEMPLATES
// 0 ACONST_NULL
// 0 INVOKESTATIC
// 0 INVOKEVIRTUAL
// 3 ICONST_0