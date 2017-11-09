package test

actual fun stringBuilder(sb: StringBuilder) {}

actual fun kotlinVersion(kv: KotlinVersion) {}

actual fun regex(r: Regex) {}

actual fun pair(p: Pair<String, List<Double>>) {}

actual fun <A, B> genericPair(p: Pair<A, B>) {}
