// FIR_IDENTICAL
// FILE: a/Foo.java
package a;

public @interface Foo{}

// FILE: b.kt
@file:Foo

package b

import a.Foo

