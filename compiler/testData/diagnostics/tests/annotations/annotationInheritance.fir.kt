val a = object: T {}
open class C
interface T

annotation class Ann: <!SUPERTYPES_FOR_ANNOTATION_CLASS!>C()<!>
annotation class Ann2: <!SUPERTYPES_FOR_ANNOTATION_CLASS!>T<!>
annotation class Ann3: <!ANNOTATION_CLASS_MEMBER, SUPERTYPES_FOR_ANNOTATION_CLASS!>T by a<!>
annotation class Ann4: <!SUPERTYPES_FOR_ANNOTATION_CLASS!>C(), T<!>
