typealias StringAlias = String

typealias SF<T> = (T) -> StringAlias

val f: SF<StringAlias> = { it }

// For proper detection of box() function, its declaration must textually match to Regex in WasmWasiBoxTestHelperSourceProvider.produceAdditionalFiles()
fun box(): StringAlias = f("OK")
