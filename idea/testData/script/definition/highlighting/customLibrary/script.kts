val c1 = C()

val c2 = C()

doStuff(c1, c2)

doStuff(c1, <error>3</error>)

// DEPENDENCIES: classpath:lib-classes; imports:custom.library.*