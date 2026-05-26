typealias StringAlias = String

typealias SF<T> = (T) -> StringAlias

val f: SF<StringAlias> = { it }

// To detect box() function in WasmWasi testinfra, its declaration must textually match to Regex in WasmWasiBoxTestHelperSourceProvider.produceAdditionalFiles()
fun box(): StringAlias = f("OK")
