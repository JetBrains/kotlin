<!EXPLICIT_METADATA_IS_DISALLOWED!>@Metadata<!>
class A

<!EXPLICIT_METADATA_IS_DISALLOWED!>@Metadata(extraString = "_")<!>
annotation class B(val m: Metadata)

<!EXPLICIT_METADATA_IS_DISALLOWED, WRONG_ANNOTATION_TARGET!>@Metadata(extraInt = 0)<!>
@B(Metadata())
fun f() {}
