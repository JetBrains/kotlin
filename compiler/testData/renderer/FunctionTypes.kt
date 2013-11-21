var v1: () -> Unit
var v2: (Int) -> Int
var v3: (Int, String) -> String
var v4: Function1<Int, String>
var v4: (() -> Int, (String) -> Unit) -> String
var v5: Int.() -> Int
var v6 : Int.(String, Int) -> Unit
var v7 : ExtensionFunction1<Int, String, Boolean>

class F: Function0<Unit>
var v8: F

class EF: ExtensionFunction0<Unit>
var v9: EF

class GF<T>: Function0<T>
var v10: GF<Any>

class GEF<A, B>: ExtensionFunction0<A, B>
var v11: GEF<Any, Any>

//internal var v1: () -> jet.Unit defined in root package
//internal var v2: (jet.Int) -> jet.Int defined in root package
//internal var v3: (jet.Int, jet.String) -> jet.String defined in root package
//internal var v4: (jet.Int) -> jet.String defined in root package
//internal var v4: (() -> jet.Int, (jet.String) -> jet.Unit) -> jet.String defined in root package
//internal var v5: jet.Int.() -> jet.Int defined in root package
//internal var v6: jet.Int.(jet.String, jet.Int) -> jet.Unit defined in root package
//internal var v7: jet.Int.(jet.String) -> jet.Boolean defined in root package
//internal final class F : () -> jet.Unit defined in root package
//public constructor F() defined in F
//internal var v8: F defined in root package
//internal final class EF defined in root package
//public constructor EF() defined in EF
//internal var v9: EF defined in root package
//internal final class GF<T> : () -> T defined in root package
//public constructor GF<T>() defined in GF
//<T> defined in GF
//internal var v10: GF<jet.Any> defined in root package
//internal final class GEF<A, B> : A.() -> B defined in root package
//public constructor GEF<A, B>() defined in GEF
//<A> defined in GEF
//<B> defined in GEF
//internal var v11: GEF<jet.Any, jet.Any> defined in root package