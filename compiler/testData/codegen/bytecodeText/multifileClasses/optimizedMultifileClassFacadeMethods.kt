// IGNORE_BACKEND: JVM_IR
// !INHERIT_MULTIFILE_PARTS
// FILE: bar.kt

@file:JvmName("Util")
@file:JvmMultifileClass
public fun bar(): String = barx()

public fun foox(): String = "O"

// FILE: foo.kt

@file:JvmName("Util")
@file:JvmMultifileClass
public fun foo(): String = foox()

public fun barx(): String = "K"

// @Util.class:
// 1 public final class Util extends Util__FooKt
// 0 public final static foo\(\)
// 0 public final static foox\(\)
// 0 public final static bar\(\)
// 0 public final static barx\(\)
