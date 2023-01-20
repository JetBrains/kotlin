import kotlin.Self
enum class EnumClass {
    <!WRONG_ANNOTATION_TARGET!>@Self<!>
    Entry
}