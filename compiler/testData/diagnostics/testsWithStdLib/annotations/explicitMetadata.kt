<!EXPLICIT_METADATA_IS_DISALLOWED!>@Metadata<!>
class A

<!EXPLICIT_METADATA_IS_DISALLOWED!>@Metadata(xs = "_")<!>
annotation class B(val m: Metadata)

<!WRONG_ANNOTATION_TARGET, EXPLICIT_METADATA_IS_DISALLOWED!>@Metadata(xi = 0)<!>
@B(Metadata())
fun f() {}
