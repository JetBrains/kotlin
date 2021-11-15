// WITH_STDLIB
// FILE: f.kt

@file:JvmName("Foo")
@file:JvmMultifileClass
<!NOT_ALL_MULTIFILE_CLASS_PARTS_ARE_JVM_SYNTHETIC!>package test<!>

fun f() {}

// FILE: g.kt

@file:JvmName("Foo")
@file:JvmMultifileClass
@file:JvmSynthetic
package test

val g = ""

// FILE: h.kt

@file:JvmName("Foo")
@file:JvmMultifileClass
<!NOT_ALL_MULTIFILE_CLASS_PARTS_ARE_JVM_SYNTHETIC!>package test<!>

fun h() {}

// FILE: z.kt

@file:JvmName("Bar")
@file:JvmMultifileClass
package test

fun z() {}
