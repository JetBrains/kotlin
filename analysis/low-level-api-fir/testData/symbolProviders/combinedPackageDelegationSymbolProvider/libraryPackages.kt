// MODULE: library1
// MODULE_KIND: LibraryBinary
// FILE: A1.kt
package library1.foo.bar

class A1

// FILE: B1.kt
package library1.foo.bar.baz

val b1: Int = 0

// FILE: C1.kt
package library.other1

fun c1() {}

// MODULE: library2
// MODULE_KIND: LibraryBinary
// FILE: A2.kt
package library2.foo.bar

class A2

// FILE: B2.kt
package library2.foo.bar.baz

val b2: Int = 0

// FILE: C2.kt
package library.other2

fun c2() {}

// MODULE: library3
// MODULE_KIND: LibraryBinary
// FILE: A3.kt
package library3

class A3

// FILE: B3.kt
package library3.foo.bar.baz

val b3: Int = 0

// FILE: C3.kt
package library.other3

fun c3() {}

// MODULE: main(library1, library2, library3)
// FILE: main.kt
package main

class Main

// HAS_PACKAGE: <root>

// HAS_PACKAGE: library1
// HAS_PACKAGE: library1.foo
// HAS_PACKAGE: library1.foo.bar
// HAS_PACKAGE: library1.foo.bar.baz
// HAS_PACKAGE: library1.foo.bar.baz.fake

// HAS_PACKAGE: library2
// HAS_PACKAGE: library2.foo
// HAS_PACKAGE: library2.foo.bar
// HAS_PACKAGE: library2.foo.bar.baz
// HAS_PACKAGE: library2.foo.bar.baz.fake

// HAS_PACKAGE: library3
// HAS_PACKAGE: library3.foo
// HAS_PACKAGE: library3.foo.bar
// HAS_PACKAGE: library3.foo.bar.baz
// HAS_PACKAGE: library3.foo.bar.baz.fake

// HAS_PACKAGE: library
// HAS_PACKAGE: library.other1
// HAS_PACKAGE: library.other2
// HAS_PACKAGE: library.other3

// HAS_PACKAGE: library.foo
// HAS_PACKAGE: library.foo.bar
// HAS_PACKAGE: library.foo.bar.baz
// HAS_PACKAGE: library.foo.bar.baz.fake

// HAS_PACKAGE: library1.other1
// HAS_PACKAGE: library2.other2
// HAS_PACKAGE: library3.other3

// HAS_PACKAGE: main
