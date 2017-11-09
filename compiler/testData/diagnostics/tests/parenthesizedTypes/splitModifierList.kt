@Target(AnnotationTarget.TYPE, AnnotationTarget.TYPE_PARAMETER)
annotation class A

@Target(AnnotationTarget.TYPE, AnnotationTarget.TYPE_PARAMETER)
annotation class B

typealias Test0 = @A @B Int
typealias Test1 = @A() (<!MODIFIER_LIST_NOT_ALLOWED!>@A<!> Int)
typealias Test2 = @A() (<!MODIFIER_LIST_NOT_ALLOWED!>@B<!> Int)
typealias Test3 = @A() (@A Int) -> Int
typealias Test4 = @A() (<!MODIFIER_LIST_NOT_ALLOWED!>@B<!> Int)?
typealias Test5 = @A() ( (<!MODIFIER_LIST_NOT_ALLOWED!>@B<!> Int)? )
typealias Test6 = (@A @B Int)
typealias Test7 = (@A @B Int)?
typealias Test8 = (@A() (<!MODIFIER_LIST_NOT_ALLOWED!>@B<!> Int)? )
typealias Test9 = (@A() (<!MODIFIER_LIST_NOT_ALLOWED!>@B<!> Int)  )?