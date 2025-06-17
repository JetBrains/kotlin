// RUN_PIPELINE_TILL: FRONTEND
// DUMP_VFIR

error object E1
error object E2
error object E3

val v1 = E1
val v2: E1 = E1
val v3: Int | E1 = E1
val v4: Int | E1 = 10
val v5: Int | E1 = <!INITIALIZER_TYPE_MISMATCH!>"10"<!>
val v6: Int = <!INITIALIZER_TYPE_MISMATCH!>"10"<!>
val v7: E1 = <!INITIALIZER_TYPE_MISMATCH!>E2<!>
val v8: E2 = <!INITIALIZER_TYPE_MISMATCH!>E1<!>
val v9: Int | E1 = <!INITIALIZER_TYPE_MISMATCH!>E2<!>
val v10: Any? | E1 = <!INITIALIZER_TYPE_MISMATCH!>E2<!>
val v11: E1 | E2 = E1
val v12: E1 | E2 = E2
val v13: E1 | E2 = <!INITIALIZER_TYPE_MISMATCH!>E3<!>
