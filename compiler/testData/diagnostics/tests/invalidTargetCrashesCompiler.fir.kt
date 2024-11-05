// ISSUE: KT-59110
// IGNORE_PHASE_VERIFICATION: invalid code inside annotations

<!WRONG_ANNOTATION_TARGET!>@Target(<!ANNOTATION_ARGUMENT_MUST_BE_CONST, ARGUMENT_TYPE_MISMATCH!>{}<!>)<!>
interface SomeInterface {
}
