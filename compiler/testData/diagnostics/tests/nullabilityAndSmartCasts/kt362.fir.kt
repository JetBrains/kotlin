// FILE: this.kt

// KT-362 Don't allow.smartcasts on vals that are not internal
package example

fun test() {
  val p = test.Public()
  if (p.public is Int) p.public + 1
  if (p.<!INVISIBLE_REFERENCE!>protected<!> is Int) p.<!INVISIBLE_REFERENCE!>protected<!> <!UNSAFE_OPERATOR_CALL!>+<!> 1
  if (p.internal is Int) p.internal + 1
  val i = test.Internal()
  if (i.public is Int) i.public + 1
  if (i.<!INVISIBLE_REFERENCE!>protected<!> is Int) i.<!INVISIBLE_REFERENCE!>protected<!> <!UNSAFE_OPERATOR_CALL!>+<!> 1
  if (i.internal is Int) i.internal + 1
}

// FILE: other.kt
package test

public class Public() {
    public val public : Int? = 1;
    protected val protected : Int? = 1;
    val internal : Int? = 1
}
internal class Internal() {
    public val public : Int? = 1;
    protected val protected : Int? = 1;
    val internal : Int? = 1
}
