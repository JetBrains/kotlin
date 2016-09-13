var v0: () -> Unit
var v1: (Int) -> Int
var v2: (number: Int, String) -> String
var v3: Function1<Int, String>
var v4: (() -> Int, (s: String) -> Unit) -> String
var v5: Int.() -> Int
var v6 : Int.(String, Int) -> Unit
var v7 : @ExtensionFunctionType Function2<Int, String, Boolean>

class F: Function0<Unit>
var v8: F

class EF: @ExtensionFunctionType Function1<String, Unit>
var v9: EF

class GF<T>: Function0<T>
var v10: GF<Any>

class GEF<A, B>: @ExtensionFunctionType Function1<A, B>
var v11: GEF<Any, Any>

var v12: (() -> Unit).() -> Unit
var v13: (() -> Unit)?.() -> Unit

//public var v0: () -> kotlin.Unit defined in root package
//public var v1: (kotlin.Int) -> kotlin.Int defined in root package
//public var v2: (number: kotlin.Int, kotlin.String) -> kotlin.String defined in root package
//public var v3: (kotlin.Int) -> kotlin.String defined in root package
//public var v4: (() -> kotlin.Int, (s: kotlin.String) -> kotlin.Unit) -> kotlin.String defined in root package
//public var v5: kotlin.Int.() -> kotlin.Int defined in root package
//public var v6: kotlin.Int.(kotlin.String, kotlin.Int) -> kotlin.Unit defined in root package
//public var v7: kotlin.Int.(kotlin.String) -> kotlin.Boolean defined in root package
//public final class F : () -> kotlin.Unit defined in root package
//public constructor F() defined in F
//public var v8: F defined in root package
//public final class EF : kotlin.String.() -> kotlin.Unit defined in root package
//public constructor EF() defined in EF
//public var v9: EF defined in root package
//public final class GF<T> : () -> T defined in root package
//public constructor GF<T>() defined in GF
//<T> defined in GF
//public var v10: GF<kotlin.Any> defined in root package
//public final class GEF<A, B> : A.() -> B defined in root package
//public constructor GEF<A, B>() defined in GEF
//<A> defined in GEF
//<B> defined in GEF
//public var v11: GEF<kotlin.Any, kotlin.Any> defined in root package
//public var v12: (() -> kotlin.Unit).() -> kotlin.Unit defined in root package
//public var v13: (() -> kotlin.Unit)?.() -> kotlin.Unit defined in root package
