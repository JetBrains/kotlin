// !DIAGNOSTICS: -UNUSED_EXPRESSION

// FILE: foo/View.java

package foo;

public class View {}

// FILE: foo/TextView.java

package foo;

public class TextView extends View {}

// FILE: k.kt

import foo.View
//import foo.TextView

fun String.gah(view:View ?) {
    if (view is <!UNRESOLVED_REFERENCE!>TextView<!>)
        view
    else <!UNRESOLVED_REFERENCE!>TextView<!>() as foo.TextView
}