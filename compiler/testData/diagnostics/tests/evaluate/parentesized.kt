// RUN_PIPELINE_TILL: FRONTEND
val p1: Byte = <!INTEGER_OPERATOR_RESOLVE_WILL_CHANGE!>(1 + 2) * 2<!>
val p2: Short = <!INTEGER_OPERATOR_RESOLVE_WILL_CHANGE!>(1 + 2) * 2<!>
val p3: Int = (1 + 2) * 2
val p4: Long = (1 + 2) * 2

val b1: Byte = <!INTEGER_OPERATOR_RESOLVE_WILL_CHANGE, TYPE_MISMATCH!>(1.toByte() + 2) * 2<!>
val b2: Short = <!INTEGER_OPERATOR_RESOLVE_WILL_CHANGE, TYPE_MISMATCH!>(1.toShort() + 2) * 2<!>
val b3: Int = (1.toInt() + 2) * 2
val b4: Long = (1.toLong() + 2) * 2

val i1: Int = (1.toByte() + 2) * 2
val i2: Int = (1.toShort() + 2) * 2

/* GENERATED_FIR_TAGS: additiveExpression, integerLiteral, multiplicativeExpression, propertyDeclaration */
