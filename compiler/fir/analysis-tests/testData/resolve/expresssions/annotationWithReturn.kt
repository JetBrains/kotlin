// Should be something like TYPE_MISMATCH here
<!WRONG_ANNOTATION_TARGET_WITH_USE_SITE_TARGET!>@file:Some(return x)<!>

const val x = 42

annotation class Some(val value: Int)
