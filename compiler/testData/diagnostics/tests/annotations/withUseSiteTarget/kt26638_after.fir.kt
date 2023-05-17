// !DIAGNOSTICS: -UNUSED_PARAMETER
// !LANGUAGE: +ProhibitRepeatedUseSiteTargetAnnotations

// Ann is not repeatable
annotation class Ann(val x: Int)

@get:Ann(10)
val a: String
    <!REPEATED_ANNOTATION!>@Ann(20)<!> get() = "foo"

@set:Ann(10)
var b: String = ""
    <!REPEATED_ANNOTATION!>@Ann(20)<!> set(value) { field = value }

<!REPEATED_ANNOTATION!>@setparam:Ann(10)<!>
var c = " "
    set(@Ann(20) x) {}

@get:Ann(10)
<!REPEATED_ANNOTATION!>@get:Ann(20)<!>
val d: String
    <!REPEATED_ANNOTATION!>@Ann(30)<!> <!REPEATED_ANNOTATION!>@Ann(40)<!> get() = "foo"
