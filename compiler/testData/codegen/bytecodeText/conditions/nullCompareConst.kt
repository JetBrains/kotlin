fun f(): Boolean = "non-primitive" == null
fun g(): Boolean = null == "non-primitive"
fun h(): Boolean = "non-primitive".equals(null)
//fun i(): Boolean = null.equals("non-primitive")
//See KT-33757

// 0 IF
// 0 ACONST_NULL
// 0 INVOKESTATIC
// 0 INVOKEVIRTUAL
// 3 ICONST_0
