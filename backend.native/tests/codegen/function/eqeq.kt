fun eqeqB  (a:Byte,   b:Byte  ) = a == b
fun eqeqS  (a:Short,  b:Short ) = a == b
fun eqeqI  (a:Int,    b:Int   ) = a == b
fun eqeqL  (a:Long,   b:Long  ) = a == b
fun eqeqF  (a:Float,  b:Float ) = a == b
fun eqeqD  (a:Double, b:Double) = a == b
fun eqeqStr(a:String, b:String) = a == b

fun eqeqeq (a: Any?, b: Any?) = a === b

fun gtI  (a:Int,    b:Int   ) = a >  b
fun ltI  (a:Int,    b:Int   ) = a <  b
fun geI  (a:Int,    b:Int   ) = a >= b
fun leI  (a:Int,    b:Int   ) = a <= b
fun neI  (a:Int,    b:Int   ) = a != b

fun gtF  (a:Float,  b:Float ) = a >  b
fun ltF  (a:Float,  b:Float ) = a <  b
fun geF  (a:Float,  b:Float ) = a >= b
fun leF  (a:Float,  b:Float ) = a <= b
fun neF  (a:Float,  b:Float ) = a != b

fun helloString()   =  "Hello"
fun goodbyeString() =  "Goodbye"