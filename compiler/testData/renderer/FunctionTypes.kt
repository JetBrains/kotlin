var v1 : () -> Unit
var v2 : (Int) -> Int
var v3 : (Int, String) -> String
var v4 : Function1<Int, String>
var v4 : (() -> Int, (String) -> Unit) -> String
var v5 : Int.() -> Int
var v6  : Int.(String, Int) -> Unit
var v7  : ExtensionFunction1<Int, String, Boolean>


//internal var v1 : () -> Unit defined in root package
//internal var v2 : (jet.Int) -> jet.Int defined in root package
//internal var v3 : (jet.Int, jet.String) -> jet.String defined in root package
//internal var v4 : (jet.Int) -> jet.String defined in root package
//internal var v4 : (() -> jet.Int, (jet.String) -> Unit) -> jet.String defined in root package
//internal var v5 : jet.Int.() -> jet.Int defined in root package
//internal var v6 : jet.Int.(jet.String, jet.Int) -> Unit defined in root package
//internal var v7 : jet.Int.(jet.String) -> jet.Boolean defined in root package
