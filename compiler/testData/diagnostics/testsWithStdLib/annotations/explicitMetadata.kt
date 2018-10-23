<!EXPLICIT_METADATA_IS_DISALLOWED!>@Metadata<!>
class A

<!EXPLICIT_METADATA_IS_DISALLOWED!>@Metadata(extraString = "_")<!>
annotation class B(val m: Metadata)

<!WRONG_ANNOTATION_TARGET, EXPLICIT_METADATA_IS_DISALLOWED!>@Metadata(extraInt = 0)<!>
@B(Metadata())
fun f() {}
