// !CHECK_TYPE

// MODULE[js]: m1
// FILE: k.kt

fun foo(dn: dynamic<!REDUNDANT_NULLABLE!>?<!>, d: dynamic, dnn: dynamic<!REDUNDANT_NULLABLE!>?<!><!REDUNDANT_NULLABLE!>?<!>) {
    dn.checkType { it : _<dynamic>}
    dn.checkType { it : _<dynamic<!REDUNDANT_NULLABLE!>?<!>>}
    d.checkType { it : _<dynamic>}
    d.checkType { it : _<dynamic<!REDUNDANT_NULLABLE!>?<!>>}
}