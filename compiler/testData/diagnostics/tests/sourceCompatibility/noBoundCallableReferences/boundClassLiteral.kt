// !LANGUAGE: -BoundCallableReferences

class C { companion object }
val ok1 = C::class
val ok2 = C.Companion::class

object O
val ok3 = O::class


val fail1 = <!UNSUPPORTED_FEATURE!>""<!>::class
val fail2 = <!CLASS_LITERAL_LHS_NOT_A_CLASS!>String?::class<!>
val fail3 = <!UNSUPPORTED_FEATURE!>(C)<!>::class
val fail4 = <!UNSUPPORTED_FEATURE!>(C.Companion)<!>::class
