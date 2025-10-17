// RUN_PIPELINE_TILL: FRONTEND
annotation class A(val a: IntArray <!INITIALIZER_TYPE_MISMATCH!>=<!> arrayOf(1))
annotation class B(val a: IntArray = intArrayOf(1))

/* GENERATED_FIR_TAGS: annotationDeclaration, collectionLiteral, integerLiteral, primaryConstructor, propertyDeclaration */
