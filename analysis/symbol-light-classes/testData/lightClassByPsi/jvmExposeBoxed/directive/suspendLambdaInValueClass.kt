// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM
// JVM_EXPOSE_BOXED

@JvmInline
value class E2Test1(val x: suspend () -> Unit)

data class E2Test2(val y: E2Test1 = E2Test1({ println(2) }))

// LIGHT_ELEMENTS_NO_DECLARATION: E2Test1.class[constructor-impl;equals-impl;equals-impl0;hashCode-impl;toString-impl], E2Test2.class[component1-9VvmrcE;copy-cc2qwR4;create;getY-9VvmrcE;invoke;invokeSuspend;label]
