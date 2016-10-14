annotation class Ann1(val p1: Int, val p2: Int)
annotation class Ann2
annotation class Ann3(val p: Int)
annotation class Ann4
annotation class Ann5
annotation class Ann6

var v1: @Ann1(1, 2) String
var v2: @Ann2 Int?
var v3: @Ann3(0) (String, Int) -> Char
var v4: @Ann4 X // currently annotations are lost for error types
var v5: @Ann5() (String.() -> Unit)
var v6: @Ann6() (() -> Unit)?

//public final annotation class Ann1 : kotlin.Annotation defined in root package
//public constructor Ann1(p1: kotlin.Int, p2: kotlin.Int) defined in Ann1
//value-parameter p1: kotlin.Int defined in Ann1.<init>
//value-parameter p2: kotlin.Int defined in Ann1.<init>
//public final annotation class Ann2 : kotlin.Annotation defined in root package
//public constructor Ann2() defined in Ann2
//public final annotation class Ann3 : kotlin.Annotation defined in root package
//public constructor Ann3(p: kotlin.Int) defined in Ann3
//value-parameter p: kotlin.Int defined in Ann3.<init>
//public final annotation class Ann4 : kotlin.Annotation defined in root package
//public constructor Ann4() defined in Ann4
//public final annotation class Ann5 : kotlin.Annotation defined in root package
//public constructor Ann5() defined in Ann5
//public final annotation class Ann6 : kotlin.Annotation defined in root package
//public constructor Ann6() defined in Ann6
//public var v1: @Ann1(p1 = 1, p2 = 2) kotlin.String defined in root package
//public var v2: @Ann2 kotlin.Int? defined in root package
//public var v3: @Ann3(p = 0) (kotlin.String, kotlin.Int) -> kotlin.Char defined in root package
//public var v4: [ERROR : X] defined in root package
//public var v5: @Ann5() (kotlin.String.() -> kotlin.Unit) defined in root package
//public var v6: @Ann6() (() -> kotlin.Unit)? defined in root package