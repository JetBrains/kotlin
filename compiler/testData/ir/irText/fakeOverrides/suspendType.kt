// SKIP_KT_DUMP

abstract class S0 : suspend () -> Unit

abstract class S2 : suspend (Int, String) -> Long

interface I1 : suspend (Int) -> Unit
interface I2 : suspend (Int) -> Unit
abstract class C : I1, I2
