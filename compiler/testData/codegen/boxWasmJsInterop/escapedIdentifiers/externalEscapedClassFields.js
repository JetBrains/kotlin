function A() {
    this["@invalid @ val@"] = 23
    this["--invalid-var"] = "A: before"
}

A.prototype["get something$weird"] = function() {
    return "something weird"
}

A["static val"] = 42
A["static var"] = "Companion: before"

A["get 🦄"] = function() {
    return "🦄"
}