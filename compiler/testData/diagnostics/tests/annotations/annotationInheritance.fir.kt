val a = object: T {}
open class C
interface T

annotation class Ann: <!SUPERTYPES_FOR_ANNOTATION_CLASS!>C()<!>
annotation class Ann2: <!SUPERTYPES_FOR_ANNOTATION_CLASS!>T<!>
annotation class Ann3: <!SUPERTYPES_FOR_ANNOTATION_CLASS!>T by <!ANNOTATION_CLASS_MEMBER!>a<!><!>
annotation class Ann4: <!SUPERTYPES_FOR_ANNOTATION_CLASS!>C(), T<!>
