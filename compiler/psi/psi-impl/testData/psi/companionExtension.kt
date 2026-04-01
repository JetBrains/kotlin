// LANGUAGE: +CompanionBlocksAndExtensions

companion fun String.foo1() {}
private companion fun String.foo2() {}
companion inline fun String.foo3() {}

companion val String.bar1 = 1
private companion val String.bar2 = 1
companion inline val String.bar3 get() = 1